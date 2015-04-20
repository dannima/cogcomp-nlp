package edu.illinois.cs.cogcomp.comma.sl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.zip.DataFormatException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.illinois.cs.cogcomp.comma.Comma;
import edu.illinois.cs.cogcomp.comma.lbj.LocalCommaClassifier$$1;
import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.illinois.cs.cogcomp.lbjava.classify.Classifier;
import edu.illinois.cs.cogcomp.lbjava.classify.FeatureVector;
import edu.illinois.cs.cogcomp.lbjava.parse.Parser;
import edu.illinois.cs.cogcomp.sl.applications.sequence.SequenceInstance;
import edu.illinois.cs.cogcomp.sl.applications.sequence.SequenceLabel;
import edu.illinois.cs.cogcomp.sl.core.SLProblem;
import edu.illinois.cs.cogcomp.sl.util.FeatureVectorBuffer;
import edu.illinois.cs.cogcomp.sl.util.IFeatureVector;
import edu.illinois.cs.cogcomp.sl.util.Lexiconer;
public class CommaIOManager {
	static Logger logger = LoggerFactory.getLogger(CommaIOManager.class);

	
	public static int numFeatures;
	public static int numLabels;
	
	public static SLProblem readProblem(Parser lbjParser, Lexiconer lm) throws IOException, Exception {
		List<String> qids = new ArrayList<String>();
		SLProblem sp = new SLProblem();
		//w:unknownword indicates out-of-vocabulary words in test phase
		if (lm.isAllowNewFeatures()){
			lm.addFeature("unknownfeature");
			lm.addLabel("occupy-zero-label-for-some-reason");
		}
		Map<String, Pair<List<Integer>, List<IFeatureVector>>> insMap = new HashMap<String, Pair<List<Integer>,List<IFeatureVector>>>();
		for(Comma comma = (Comma) lbjParser.next(); comma!=null; comma = (Comma) lbjParser.next()){
			String line = commaToSLString(comma);
			Iterator<String> st = Arrays.asList(line.split("\\s+")).iterator();
			String token;
			if(!line.contains("qid"))
				throw new Exception("invalid input String or empty line(does not contain 'qid'): "+ line);

			try {
				//label string
				token = st.next();
			} catch (NoSuchElementException e) {
				throw new Exception("empty line", e);
			}
			lm.addLabel(token);
			int label = lm.getLabelId(token);
			
			if(label <= 0)
				throw new Exception("label Id should start from 1????)");
			
			token = st.next();
			String qid = token.split(":")[1];

			if(!insMap.containsKey(qid)){
				qids.add(qid);
				insMap.put(qid, new Pair<List<Integer>, List<IFeatureVector>>(new ArrayList<Integer>(), new ArrayList<IFeatureVector>()));
			}

			FeatureVectorBuffer fvb = new FeatureVectorBuffer();

			while(st.hasNext()){
				String feature = st.next();
				//float value = Float.parseFloat(st.nextToken());
				float value = 1.0f;
				int index = -1;
				
				
				
				
				if (lm.isAllowNewFeatures()) {
					lm.addFeature(feature);
				}
				if (lm.containFeature(feature))
					index = lm.getFeatureId(feature);
				else
					index = lm.getFeatureId("unknownfeature");
				
				
//				indeices.add(index);
				fvb.addFeature(index, value);
			}
			
			// put label
			insMap.get(qid).getFirst().add(label);
			insMap.get(qid).getSecond().add(fvb.toFeatureVector());
		}
		for(String qid : qids){
			Pair<List<Integer>, List<IFeatureVector>> ins = insMap.get(qid);				
			List<IFeatureVector> fvs = ins.getSecond();
			List<Integer> labels = ins.getFirst();
			if (labels.size() != fvs.size()) {
				throw new DataFormatException(
						"The number of tokens and number tags in does not match\n"
								+ "Error sampls:\n"
								+ insMap.get(qid));
			}
			
			int[] labelArray = new int[labels.size()];
			SequenceInstance seq = new SequenceInstance(fvs.toArray(new IFeatureVector[fvs.size()]));
			sp.instanceList.add(seq);
			for(int i=0; i<labels.size(); i++)
				labelArray[i] = labels.get(i);
			sp.goldStructureList.add(new SequenceLabel(labelArray));
		}
		
		
		if(lm.isAllowNewFeatures()){
			//TODO I THINK I MAY NEED TO SUBTRACT 1 FROM NUM FEATURES ?
			numFeatures = lm.getNumOfFeature();
			numLabels = lm.getNumOfLabels();
		}
		
		return sp;
	}
	
	public static int LabelStringToInt(String labelString) throws Exception{
		int labelInt = -1;
		if(labelString.equals("Attribute"))
			labelInt = 1111;
		else if(labelString.equals("Substitute"))
			labelInt = 2222;
		else if(labelString.equals("Locative"))
			labelInt = 3333;
		else if(labelString.equals("List"))
			labelInt = 4444;
		else if(labelString.equals("Other"))
			labelInt = 5555;
		else 
			throw new Exception("invalid label: " + labelString);
		return labelInt;
	}
	
	public static String LabelIntToString(int labelInt) throws Exception {
		String labelString = "?????";
		switch (labelInt) {
		case 1111:
			labelString = "Attribute";
			break;
		case 2222:
			labelString = "Substitute";
			break;
		case 3333:
			labelString = "Locative";
			break;
		case 4444:
			labelString = "List";
			break;
		case 5555:
			labelString = "Other";
			break;
		default:
			throw new Exception("invalid label int: " + labelInt);
		}
		return labelString;
	}
	
	public static String commaToSLString(Comma comma){
		Classifier extractor = new LocalCommaClassifier$$1();
		StringBuffer line = new StringBuffer();
		line.append(comma.getRole());
		//line.append(" qid:" + comma.getSentence().hashCode());
		line.append(" qid:" + comma.getSiblingCommaHead().hashCode());
		FeatureVector fv = extractor.classify(comma);
		for(int i = 0; i < fv.featuresSize(); i++)
			line.append(" " + fv.getFeature(i).toStringNoPackage().replaceAll("\\s+", ""));
		return line.toString();
	}
}
