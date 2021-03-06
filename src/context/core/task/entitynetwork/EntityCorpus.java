/**
 * 
 */
package context.core.task.entitynetwork;

import context.core.entity.CorpusData;
import context.core.entity.FileData;
import context.core.entity.TabularData;
import context.core.textnets.Corpus;
import context.core.textnets.Corpus.LABELTYPES;
import context.core.textnets.Network;
import context.core.textnets.Network.FileType;
import context.core.util.JavaIO;
import context.core.task.entitydetection.MultiWordEntities;
import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.apache.commons.io.FilenameUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import context.core.util.CorpusAggregator;
import context.core.util.ForAggregation;
import context.core.entity.CorpusData;
import context.core.entity.FileData;
import context.core.entity.TabularData;
import context.core.util.CorpusAggregator;
import context.core.util.ForAggregation;
import context.core.util.JavaIO;
import edu.stanford.nlp.ie.AbstractSequenceClassifier;



/**
 * @author Shubhanshu
 *
 */
public class EntityCorpus extends Corpus {

	/**
	 * 
	 */
	private EntityNetworkTaskInstance instance;
	private CorpusData input;
	private CorpusData output;
    private List<TabularData> tabularOutput;
	
	private AbstractSequenceClassifier<CoreLabel> classifier3;
    private AbstractSequenceClassifier<CoreLabel> classifier4;
    private AbstractSequenceClassifier<CoreLabel> classifier7;
	private StanfordCoreNLP pipeline;
	
	List<String[]> entitiesWithCount;

	
    /**
     *
     * @param serializedClassifier
     */
    public EntityCorpus(String serializedClassifier) {
		// TODO Auto-generated constructor stub
		try {
			classifier7 = CRFClassifier.getClassifier(serializedClassifier);
		} catch (ClassCastException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
    /**
     *
     * @param instance
     */
    public EntityCorpus(EntityNetworkTaskInstance instance) {
		// TODO Auto-generated constructor stub
		super();
		this.instance = instance;
		this.input = (CorpusData) instance.getInput();
		this.output = (CorpusData) instance.getTextOutput();
        this.tabularOutput = instance.getTabularOutput();
        this.classifier3 = instance.get3Classifier();
        this.classifier4 = instance.get4Classifier();
        this.classifier7 = instance.get7Classifier();
        this.setLabels(classifier7.labels(), LABELTYPES.ENTITY);
        // TODO Add filter labels based on GUI Instance values.
        this.setFilterLabels(instance.getFilterLabels());
        this.setUnit(UNITOFANALYSIS.values()[instance.getUnitOfAnalysis()-1]);
        this.setWindowSize(instance.getDistance());
        //System.err.println("Window Size: "+instance.getDistance()+", "+this.getWindowSize());
        //System.out.println("Harathi in EntityCorpus.java constructor");
        this.setDocLevel(false);
		
		
	}
	
    /**
     *
     */
    public void genStreamsFromCorpus(){
	    detectEntities();
		List<FileData> files = input.getFiles();
		for (FileData f:files){
			File file = f.getFile();
			String text = "";
			try {
				text = JavaIO.readFile(file);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			text = text.replaceAll("\\p{Cc}", " ");
            text = text.replaceAll("[^A-Za-z0-9 :;!\\?\\.,\'\"-]", " ");
            //System.out.println("Harathi in EntityCorpus.java text:"+text);
            String fileName = f.getName().getValue();
            System.out.println("Harathi in entity corpus filename:"+fileName);
            System.out.println("Harathi in entity corpus text"+text);
            EntityTextStream ets = getEntityTextStream(fileName, text);
            this.addStream(fileName, ets);
            //System.err.println("Finished adding Text Stream: "+fileName);
            
            String htmlString = classifier7.classifyToString(text, "inlineXML", true);
            String inputNameWithoutExtension = FilenameUtils.getBaseName(f.getFile().getName());
            String inputExtension = FilenameUtils.getExtension(f.getFile().getName());
            String outputFile = inputNameWithoutExtension + "-ED." + inputExtension;
            int index = output.addFile(outputFile);
            output.writeFile(index, htmlString.toString());
            //System.err.println("Finished Writing the Modified File: "+fileName);
            
		}
	}
	
    /**
     *
     * @param fileName
     * @param inText
     * @return
     */
    public EntityTextStream getEntityTextStream(String fileName, String inText){
    	EntityTextStream ets = new EntityTextStream(fileName);
    	List<List<CoreLabel>> out = classifier7.classify(inText);
    	//System.out.println("Harathi in EntityCorpus.java inText:"+inText);
    	//System.out.println("Harathi in EntityCorpus.java classifer7labels:"+classifier7.labels());
    	for (List<CoreLabel> sentence : out) {
            for (int i = 0; i < sentence.size(); i++) {
            	CoreLabel word = sentence.get(i);
            	//System.out.println("Harathi CoreLabel:"+word);
            	String wordStr = word.word();
            	System.out.println("Harathi wordStd:"+wordStr);
            	String wordLbl = word.get(new CoreAnnotations.AnswerAnnotation().getClass());
            	System.out.println("Harathi wordlabel:"+wordLbl);
            	for (int i1 = 0; i1 < entitiesWithCount.size(); i1++) {
					//System.out.println("word: " + entitiesWithCount.get(i1)[0]+ " word label: "+entitiesWithCount.get(i1)[1]+ " our count: "+count);
					System.out.println("comparing word: " + wordStr+ " with enitities list : "+entitiesWithCount.get(i1)[0]);
					if(wordStr.equals(entitiesWithCount.get(i1)[0]))
						{
						   System.out.println("matched setting label to : " +entitiesWithCount.get(i1)[1]);
						   wordLbl = entitiesWithCount.get(i1)[1];
						}						
				}
				System.out.println("Harathi wordlabel after modification:"+wordLbl);
            	if(!wordLbl.equals("O")){
            		int j = i+1;
                	String nextLbl = null;
                	CoreLabel nextWord;
            		String nextStr;
                	while(j<sentence.size()){
                		nextWord = sentence.get(j);
                		nextStr = nextWord.word();
                		nextLbl = nextWord.getString(CoreAnnotations.AnswerAnnotation.class);
                		if(!nextLbl.equals(wordLbl)){
                			break;
                		}
                		wordStr += "_"+nextStr;
                		j++;
                	}
                	i = j-1;
            	}
            	//System.out.print("Harathi wordstr"+wordStr+ '/' + wordLbl + ' ');
            	EntityToken e = new EntityToken(wordStr);
            	if(wordLbl.equals("O") || ! this.getFilterLabels().contains(wordLbl)){
            		wordLbl = null;
            	}
            	e.setLabel(wordLbl, LABELTYPES.ENTITY);
            	ets.addToken(e);
            }
            //System.out.println();
          }
    	System.out.println("Harathi Done");
		
    	return ets;
    	
    }
	
    /**
     *
     * @param outputDir
     */
    public void saveNetworks(String outputDir){
		String fileName = "EntityNetwork";
		saveNetworks(fileName, outputDir, FileType.GRAPHML, LABELTYPES.ENTITY);
		
	}
	
	
	
	//adding new code by harathi for detecting entities
	/**
     *
     * @return
     */
    public boolean detectEntities() {
        System.out.println("start detectEntities in Entity Corpus...");
        List<FileData> files = input.getFiles();

        List<List<String[]>> toAggregate = new ArrayList<List<String[]>>();
        try {
            for (FileData ff : files) {

                File file = ff.getFile();
                String text;
                try {
                    text = JavaIO.readFile(file);
                    text = text.replaceAll("\\p{Cc}", " ");
                    text = text.replaceAll("[^A-Za-z0-9 :;!\\?\\.,\'\"-]", " ");
                    System.out.println("Harathi in entity:"+text);
                    List<String[]> longEntities = new ArrayList<String[]>();

                    List<ForAggregation> longEntities3 = new ArrayList<ForAggregation>();
                    List<ForAggregation> longEntities4 = new ArrayList<ForAggregation>();
                    List<ForAggregation> longEntities7 = new ArrayList<ForAggregation>();
                    MultiWordEntities MWE3 = MultiWordEntityRecognition(classifier3, text);
                    MultiWordEntities MWE4 = MultiWordEntityRecognition(classifier4, text);
                    MultiWordEntities MWE7 = MultiWordEntityRecognition(classifier7, text);

                    longEntities3.addAll(MWE3.forAgg);
                    //longEntities4.addAll(MWE4.forAgg);
                    longEntities7.addAll(MWE7.forAgg);

                    HashMap<String, Integer[]> Entities = new HashMap<String, Integer[]>();

                    for (int entityIndex = 0; entityIndex < longEntities3.size(); entityIndex++) {
                        Integer[] offsetArray = {MWE3.startInd.get(entityIndex)};
                        Entities.put(longEntities3.get(entityIndex).toAggregate[0], offsetArray);
                        longEntities.add(longEntities3.get(entityIndex).toAggregate);
                    }
                    // The following code is to incorporate another NER library, but it was causing
                    // problems for large individual documents, so for now "longEntities4.addAll(MWE4.forAgg);" 
                    //has been commented out.  In the future,
                    // a check on file size may be done to switch between using the library or not.

                    for (int entityIndex = 0; entityIndex < longEntities4.size(); entityIndex++) {

                        if (Entities.containsKey(longEntities4.get(entityIndex).toAggregate[0]) && Arrays.asList(Entities.get(longEntities4.get(entityIndex).toAggregate[0])).contains(MWE4.startInd.get(entityIndex))) {
                            continue;
                        } else if (Entities.containsKey(longEntities4.get(entityIndex).toAggregate[0])) {
                            Integer[] numOfOcc = Entities.get(longEntities4.get(entityIndex).toAggregate[0]);
                            Integer[] offsetArray4 = new Integer[numOfOcc.length + 1];
                            offsetArray4 = Arrays.copyOf(Entities.get(longEntities4.get(entityIndex).toAggregate[0]), offsetArray4.length);
                            offsetArray4[offsetArray4.length - 1] = MWE4.startInd.get(entityIndex);
                            Entities.put(longEntities4.get(entityIndex).toAggregate[0], offsetArray4);
                            System.out.println("Harathi in first if longentities:"+longEntities4.get(entityIndex).toAggregate[0]);
                            System.out.println("Harathi in first if offset:"+offsetArray4);
                            longEntities.add(longEntities4.get(entityIndex).toAggregate);
                        } else {
                            Integer[] offsetArray = {MWE4.startInd.get(entityIndex)};
                            Entities.put(longEntities4.get(entityIndex).toAggregate[0], offsetArray);
                            System.out.println("Harathi in first else longentities:"+longEntities4.get(entityIndex).toAggregate[0]);
                            System.out.println("Harathi in first else offset:"+offsetArray);
                            longEntities.add(longEntities4.get(entityIndex).toAggregate);
                        }

                    }

                    for (int entityIndex = 0; entityIndex < longEntities7.size(); entityIndex++) {

                        if (Entities.containsKey(longEntities7.get(entityIndex).toAggregate[0]) && Arrays.asList(Entities.get(longEntities7.get(entityIndex).toAggregate[0])).contains(MWE7.startInd.get(entityIndex))) {
                            continue;
                        } else if (Entities.containsKey(longEntities7.get(entityIndex).toAggregate[0])) {
                            Integer[] numOfOcc = Entities.get(longEntities7.get(entityIndex).toAggregate[0]);
                            Integer[] offsetArray7 = new Integer[numOfOcc.length + 1];
                            offsetArray7 = Arrays.copyOf(Entities.get(longEntities7.get(entityIndex).toAggregate[0]), offsetArray7.length);
                            offsetArray7[offsetArray7.length - 1] = MWE7.startInd.get(entityIndex);
                            Entities.put(longEntities7.get(entityIndex).toAggregate[0], offsetArray7);
                            System.out.println("Harathi in second if longentities:"+longEntities7.get(entityIndex).toAggregate[0]);
                            System.out.println("Harathi in secondif offset:"+offsetArray7);
                            longEntities.add(longEntities7.get(entityIndex).toAggregate);
                        } 
                         else {
                            Integer[] offsetArray = {MWE7.startInd.get(entityIndex)};
                            Entities.put(longEntities7.get(entityIndex).toAggregate[0], offsetArray);
                            System.out.println("Harathi in second else longentities:"+longEntities7.get(entityIndex).toAggregate[0]);
                            System.out.println("Harathi in second else offset:"+offsetArray);
                            longEntities.add(longEntities7.get(entityIndex).toAggregate);
                        }
                    }

                    toAggregate.add(longEntities);

                } catch (IOException e) {
                    e.printStackTrace();
                    return false;
                }
            }

            //List<String[]> EntitiesToBeRemoved = new ArrayList<String[]>();
            List<String[]> entitiesWithCount = new CorpusAggregator().CorpusAggregate(toAggregate);
            /*
             for (String[] entityWithCount : entitiesWithCount) {
             //                try {
             if (entityWithCount[0].split(" ").length == 1 // && JavaIO.readFile(stopFile).contains(entityWithCount[0].toLowerCase()) ) {
             EntitiesToBeRemoved.add(entityWithCount);
             }
             //                } catch (IOException e) {
             //                    e.printStackTrace();
             //                    return false;
             //                }
             }
             for (String[] entityWithCount : EntitiesToBeRemoved) {
             entitiesWithCount.remove(entityWithCount);
             }

             */
				for (int i1 = 0; i1 < entitiesWithCount.size(); i1++) {
				String findStr = entitiesWithCount.get(i1)[2];
				int count = 0;
				for (FileData ff : files) {
                File file = ff.getFile();
                String text;
                text = JavaIO.readFile(file);
                text = text.replaceAll("\\p{Cc}", " ");
                text = text.replaceAll("[^A-Za-z0-9 :;!\\?\\.,\'\"-]", " ");
				String str = text;
				int lastIndex = 0;
				while(lastIndex != -1){
				lastIndex = str.indexOf(findStr,lastIndex);
					if(lastIndex != -1){
					count ++;
					lastIndex += findStr.length();
					}
				}
			    }
				entitiesWithCount.get(i1)[2] = count + "";
				System.out.println("word: " + entitiesWithCount.get(i1)[0]+ " word label: "+entitiesWithCount.get(i1)[1]+ " our count: "+count);
				}
            this.entitiesWithCount = entitiesWithCount;			
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private MultiWordEntities MultiWordEntityRecognition(AbstractSequenceClassifier<?> classifier, String inText) {

        List<ForAggregation> NamedEntities = new ArrayList<ForAggregation>();
        String htmlString = classifier.classifyToString(inText, "inlineXML", true);
        Pattern tags = Pattern.compile("<.+?>.+?</.+?>");
        Pattern tempTags = null;
        Matcher matcher = tags.matcher(htmlString);
        Matcher tempMatcher = null;
        List<Integer> startIndicies = new ArrayList<Integer>();
        HashMap<String, Integer> hashedNumOcc = new HashMap<String, Integer>();

        while (matcher.find()) {
            String name = (matcher.group().replaceAll("<.+?>", ""));
            /*
             if (name.split("\\s+").length<2){
             continue;
             }
             */
            String[] NamedEntity_array = {name.trim().replaceAll(" +", " "), matcher.group().replaceAll("<", "").replaceAll(">.+", "")};

            if (hashedNumOcc.containsKey(name)) {
                hashedNumOcc.put(name, hashedNumOcc.get(name) + 1);
            } else {
                hashedNumOcc.put(name, 1);
            }
            ForAggregation NamedEntity = new ForAggregation(NamedEntity_array);
            startIndicies.add(findNthIndexOf(inText, name, hashedNumOcc.get(name)));
            if (null != NamedEntity) {
                NamedEntities.add(NamedEntity);
            }

        }
        MultiWordEntities toReturn = new MultiWordEntities(NamedEntities, startIndicies);
        return toReturn;
    }

    private int findNthIndexOf(String str, String needle, int occurence)
            throws IndexOutOfBoundsException {
        int index = -1;
        Pattern p = Pattern.compile(needle, Pattern.MULTILINE);
        Matcher m = p.matcher(str);
        while (m.find()) {
            if (--occurence == 0) {
                index = m.start();
                break;
            }
        }
        if (index < 0) {
//            throw new IndexOutOfBoundsException();
            return 0; 
        }
        return index;
    }

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String text = "Values and Harassment Prevention.\r\n" + 
				"Dear Mr. Lay, I understand that Cindy Olson talked with you about the harassment prevention training scheduled over the next few weeks. In past classes, a senior-level business person has emphasized that harassment is not tolerated and is not consistent with Enrons values. For this set of training classes, the team asked if you could deliver that message via videotape. Cindy told me that your answer is \"yes.\" I understand that you are scheduled to record an unrelated video on Thursday morning, and that it might be possible to tape this message at the end of that session. Would that work with your schedule? If so, I will provide you with talking points and a script. If another time is better for you, let me know. Thanks for your help! Michelle Cash.";
		text += " "+text;
		text = text.replaceAll("\\p{Cc}", " ");
        text = text.replaceAll("[^A-Za-z0-9 :;!\\?\\.,\'\"-]", " ");
		String serializedClassifier = "edu/stanford/nlp/models/ner/english.muc.7class.distsim.crf.ser.gz";
		EntityCorpus ec = new EntityCorpus(serializedClassifier);
		EntityTextStream ets = ec.getEntityTextStream("", text);
		Network net = new Network();
		ets.makeNetwork(net, 7, LABELTYPES.ENTITY, UNITOFANALYSIS.SENTENCE);
		net.saveNet("outputNet", "./", FileType.GRAPHML);
				

	}

}
