package edu.indiana.d2i.htrc.randomsampling.tree;

import java.util.*;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import edu.indiana.d2i.htrc.randomsampling.exceptions.SampleNumberTooLarge;

/**
 * This is the core part of the random sampling. It represents the category of
 * library of congress (LOCC) as a tree. The tree is designed as a trie. An
 * example representation is as follows.
 * <p>
 * 
 *              Q                 <br>
 *             /                  <br>
 *            QH                  <br>
 *          /    \                <br>
 *      1-278.5  301-705.5        <br>
 *     /      \                   <br>
 *  1-199.5 201-278.5             <br>
 * 
 * <p>
 * A query string is broken into a letter part and a range part. The query is
 * similar to a trie query but not exactly the same. The letter part of the
 * query string is the same as the trie query. For the range part, the range
 * does not get dropped. Instead the search procedure carries the range part to
 * the children.
 * 
 * @see <a href="http://www.loc.gov/catdir/cpso/lcco/">Library of Congress
 *      Classification Outline</a>
 */
class CategoryNode {
	static Pattern rangePattern = Pattern.compile("\\d+(\\.\\d+)?-\\d+(\\.\\d+)?");
	static Pattern digitPattern = Pattern.compile("\\d+(\\.\\d+)?");
	static Pattern categoryPattern = Pattern.compile("[A-Z]+\\d+(\\.\\d+)?|[A-Z]+\\d+(\\.\\d+)?-\\d+(\\.\\d+)?");
	
	private static Logger logger = Logger.getLogger(CategoryNode.class);
	
	class Range {
		float min = -1;
		float max = -1;
		
		public Range(float min, float max) {
			this.min = min;
			this.max = max;
		}
		
		public String toString() {
			return String.format("[%f, %f]", min, max);
		}
	}

	class Children {
		private List<String> prefix = new ArrayList<String>();
		private List<Range> ranges = new ArrayList<Range>();
		
		private List<CategoryNode> childNodes = new ArrayList<CategoryNode>();
		
		/** It is used to insert tree nodes. [1, 295] will be viewed as the same as [1, 300] */
		protected int getIndex(Category category) {
			if (category.isLetter == true) {
				return Collections.binarySearch(prefix, category.prefixStr);
			} else {				
				return Collections.binarySearch(ranges, category.range, new Comparator<Range>() {
					@Override
					public int compare(Range r1, Range r2) {
						if ((r1.min >= r2.min && r1.max <= r2.max) || 
							(r2.min >= r1.min && r2.max <= r1.max)) {
							return 0;
						} else if (r1.max < r2.max) {
							return -1;
						} else {
							return 1;
						}
					}					
				});
			}
		}
		
		public void updateChild(Category category) {
			int index = getIndex(category);
			String subStr = category.suffixStr;
			if (index >= 0) {
				if (category.isLetter == true && subStr != null) childNodes.get(index).addCategory(subStr);
				else childNodes.get(index).addCategory(category.toString());
			} else {
				index = -(index + 1);
				if (category.isLetter == true) {
					prefix.add(index, category.prefixStr);
				} else {
					ranges.add(index, category.range);
				}
				childNodes.add(index, new CategoryNode(category.prefixStr));
				
				if (category.isLetter == true && subStr != null) childNodes.get(index).addCategory(subStr);
			}
		}
		
		/**
		 * It tries to find an exact match for the category. If the query string
		 * does not match any category, it tries to find the closest i.e. the lowest
		 * node in the tree that matches the query string.
		 */
		public CategoryNode getChild(Category category) {
			int index = getIndex(category);
			if (index < 0) {
//				throw new IllegalArgumentException(category + " does not exist!");
				return null;
			}
			
			CategoryNode child = childNodes.get(index);
			if (child.children.childNodes.size() == 0) {
				return child;
			} else if (category.isLetter == true) {
				return (category.suffixStr == null) ? child: child.find(category.suffixStr);
			} else {
				if (child.categoryStr.equals(category.prefixStr)) {
					return child;
				} else {
					CategoryNode res = child.find(category.prefixStr);
					return (res != null) ? res: child;				
				}				
//				return (child.categoryStr.equals(category.prefixStr)) ? child: child.find(category.prefixStr); 
			}
		}
	}
	
	/**
	 * This class parses the category string. Because a category in the tree could
	 * be a letter or a range, the isLetter field is used to tell if the category
	 * is a letter or a range.
	 */
	class Category {
		String str; // the whole string
		
		String prefixStr = null; 
		String suffixStr = null;
		Range range = null; 
		boolean isLetter = false; // whether this category is a letter or a range

		public Category(String str) {
			this.str = str;
			if (rangePattern.matcher(str).matches()) {
				String[] item = str.split("-");
				range = new Range(Float.parseFloat(item[0]), Float.parseFloat(item[1]));
				this.prefixStr = str;
			} else if (digitPattern.matcher(str).matches()) {
				range = new Range(Float.parseFloat(str), Float.parseFloat(str));
				this.prefixStr = str;
			} else {
				isLetter = true;
				this.prefixStr = str.substring(0, 1);
				this.suffixStr = (str.length() > 1) ? str.substring(1): null;
			}
		}

		public String getCurrentCategoryStr() {
			return this.prefixStr;
		}

		public String getSubCategoryStr() {
			return this.suffixStr;
		}
		
		public String toString() {
			return this.str;
		}
	}

	private Children children = new Children();
	private String categoryStr = null;	

	//
	private List<String> idList = new ArrayList<String>();
	
	// unit test purpose only
	protected CategoryNode() {}
	
	// unit test purpose only
	protected int childrenCount() {
		return this.children.childNodes.size();
	}
	
	/** It returns the total number of id under this node. */
	protected int idCount() {
		int sum = 0;
		for (CategoryNode child : this.children.childNodes) {
			sum += child.idCount();
		}
		return sum + idList.size();
	}
	
	/** pick random samples */
	private List<String> randomsample(int samples) {
		if (samples == 0 || idList.size() == 0) {
			return new ArrayList<String>();
		} else {
			Collections.shuffle(idList);
			return idList.subList(0, samples);
		}
	}
	
	public static boolean validateCategoryString(String categoryStr) {
		return categoryPattern.matcher(categoryStr).matches();
	}
	
	public CategoryNode(String category) {
		this.categoryStr = category;
	}

	public void addCategory(String str) {
		Category category = new Category(str);
		children.updateChild(category);
	}

	public CategoryNode find(String category) {
		return children.getChild(new Category(category));
	}
	
	public void addId(String volumeID) {
		this.idList.add(volumeID);
	}

	public List<String> samples(int sampleNum) throws SampleNumberTooLarge {
		// calculate the #samples for each child
		double total = idList.size();
		int[] idcount = new int[children.childNodes.size() + 1];
		idcount[0] = idList.size();
		for (int i = 1; i < idcount.length; i++) {
			idcount[i] = children.childNodes.get(i-1).idCount();
			total += idcount[i];
		}
		
		if (total < sampleNum) {
			throw new SampleNumberTooLarge(String.format(
				"Sampling number %d is larger than the total number %d", sampleNum, (int)total));
		}
		
		// calculate the cdf [current node, children]
		double[] cdf = new double[children.childNodes.size() + 1];
		cdf[0] = idList.size() / total;
		for (int i = 1; i < cdf.length; i++) {
			cdf[i] = cdf[i-1] + children.childNodes.get(i-1).idCount()/total;
		}		
		logger.debug("CDF: " + Arrays.toString(cdf));
		
		// 
		int[] samples = new int[children.childNodes.size() + 1];
		for (int i = 0; i < sampleNum; i++) {
			double dice = Math.random();
			int low = 0; 
			int high = cdf.length - 1;
			while (low < high) {
				int mid = (low + high) / 2;
				if (cdf[mid] >= dice) {
					high = mid;
				} else {
					low = mid + 1;
				}
			}
			samples[low]++;
		}
		logger.debug("Samples: " + Arrays.toString(samples) + " in " + this.categoryStr);
		
		// random sampling
		List<String> volumes = new ArrayList<String>();
		for (int i = 1; i < samples.length; i++) {
			volumes.addAll(children.childNodes.get(i-1).samples(samples[i]));
		}
		volumes.addAll(randomsample(samples[0]));
		
		return volumes;
	}
	
	public String toString() {
		return this.categoryStr;
	}
}
