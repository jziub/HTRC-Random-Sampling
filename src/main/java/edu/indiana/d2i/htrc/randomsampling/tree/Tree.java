package edu.indiana.d2i.htrc.randomsampling.tree;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import org.apache.log4j.Logger;

import edu.indiana.d2i.htrc.randomsampling.Configuration;
import edu.indiana.d2i.htrc.randomsampling.exceptions.NoCategoryFound;
import edu.indiana.d2i.htrc.randomsampling.exceptions.SampleNumberTooLarge;

public class Tree {
	private static Logger logger = Logger.getLogger(Tree.class);
	private static Tree instance;
	private final CategoryNode root;
//	private final Configuration conf;
	
	// TODO: check line format
	private void loadVolumeId(String path) {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(path));
			String line = null;
			// <callno, <id1, id2, ...>>
			Map<String, List<String>> categoryMapping = new HashMap<String, List<String>>();
			
			// an example line is like: 
			// uc2.ark:/13960/t57d2rr1p        ['QH81 .W68', 'QH81 .W56']
			int total = 0;
			int discard = 0;
			while ((line = reader.readLine()) != null) {
				int pivot = line.indexOf("[");
				int last = line.indexOf("]");
				String id = line.substring(0, pivot).trim();
				String[] callnums = line.substring(pivot+1, last).trim().split(",");
				boolean validCategory = false;
				for (String callnum : callnums) {
					String categoryStr = callnum.substring(1, callnum.length()-1).split("\\.")[0].trim();
					if (CategoryNode.validateCategoryString(categoryStr)) {
						if (!categoryMapping.containsKey(categoryStr)) {
							categoryMapping.put(categoryStr, new ArrayList<String>());
						}
						categoryMapping.get(categoryStr).add(id);
						validCategory = true;
					}
				}
				
				total++;
				if (!validCategory) discard++;
			}			
			reader.close();
			
			int idCnt = 0;
			for (Map.Entry<String, List<String>> entry : categoryMapping.entrySet()) {
				String categoryStr = entry.getKey();
				List<String> idlist = entry.getValue();
				CategoryNode node = root.find(categoryStr);
				if (node != null) {
					for (String id : idlist) node.addId(id);
					idCnt += idlist.size();
				} else {
					logger.warn(categoryStr + " is not found!");
				}
			}
			
			idCntInserted = idCnt;
			logger.info(String.format("Total #id: %d, #id discarded: %d, #id inserted: %d", total, discard, idCnt));
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}
	
	private Tree(Configuration conf) {
		// build the structure
		root = new CategoryNode();
		root.addCategory("QH1-278.5");
		root.addCategory("QH1-199.5");
		root.addCategory("QH201-278.5");
		root.addCategory("QH301-705.5");
		root.addCategory("QH359-425");
		root.addCategory("QH426-470");
		root.addCategory("QH471-489");
		root.addCategory("QH501-531");		
		root.addCategory("QH540-549.5");
		root.addCategory("QH573-671");
		root.addCategory("QH705-705.5");
		
		// load the volume id
//		this.conf = conf;
		loadVolumeId(conf.getString(Configuration.PropertyNames.VOLUME_CALLNO));
	}
	
  // unit test purpose only
	protected int idCount() {
		return root.idCount();
	}
	
	// unit test purpose only
	protected int idCntInserted = 0;
	
	public synchronized static Tree getSingelton(Configuration conf) {
		if (instance == null) {
			instance = new Tree(conf);
		}		
		return instance;
	}
	
	public int findIdCount(String categoryStr) throws NoCategoryFound {
		CategoryNode node = root.find(categoryStr);  
		if (node == null) {
			throw new NoCategoryFound(categoryStr + " is not found!");
		} else {
			return node.idCount();
		}
	}
	
	public List<String> randomSampling(String category, int number) 
		throws NoCategoryFound, SampleNumberTooLarge {
		CategoryNode node = root.find(category);
		if (node == null) {
			throw new NoCategoryFound(category + " is not found!");
		} else {
			return node.samples(number);
		}
	}
}
