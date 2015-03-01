package edu.indiana.d2i.htrc.randomsampling.tree;

import java.util.*;
import java.util.regex.Matcher;
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
	static Pattern letterPattern = Pattern.compile("[A-Z]+");
	static Pattern categoryPattern = Pattern.compile("[A-Z]+\\d+(\\.\\d+)?|[A-Z]+\\d+(\\.\\d+)?-\\d+(\\.\\d+)?");
	
	private static Logger logger = Logger.getLogger(CategoryNode.class);

	static class Range implements Comparable<Range> {
		float min = -1;
		float max = -1;
		
		public Range(float min, float max) {
			this.min = min;
			this.max = max;
		}
		
		public String toString() {
			return String.format("[%f, %f]", min, max);
		}

		@Override
		public int compareTo(Range cmp) {
			if (this.min < cmp.min) {
				return -1;
			} else if (this.min > cmp.min) {
				return 1;
			} else {
				if (this.max < cmp.max) {
					return -1;
				} else if (this.max > cmp.max) {
					return 1;
				} else {
					return 0;
				}
			}
		}
		
		public static Range valueOf(String str) {
			if (rangePattern.matcher(str).matches()) {
				String[] item = str.split("-");
				return new Range(Float.parseFloat(item[0]), Float.parseFloat(item[1]));
			} else if (digitPattern.matcher(str).matches()) {
				return new Range(Float.parseFloat(str), Float.parseFloat(str));
			} else {
				return null;
			}
		}
	}
	
	class Children {	
		private Map<String, List<Integer>> letterIndices = new HashMap<String, List<Integer>>();
		private Map<Range, Integer> rangeIndices = new HashMap<Range, Integer>();		
		
		private List<CategoryNode> childNodes = new ArrayList<CategoryNode>();
		
		public void addChild(Category category, CategoryNode child) {			
			childNodes.add(child);
			
			if (category.isLetter) {
				if (!letterIndices.containsKey(category.prefixStr)) {
					letterIndices.put(category.prefixStr, new ArrayList<Integer>());
				}			
				letterIndices.get(category.prefixStr).add(childNodes.size()-1);
			} else {
				rangeIndices.put(category.range, childNodes.size()-1);
			}
		}
		
		/**
		 * It tries to find an exact match for the category. If the query string
		 * does not match any category, it tries to find the closest i.e. the lowest
		 * node in the tree that matches the query string.
		 */
		public CategoryNode getParent(Category category) {			
			if (category.isLetter) {
				// digit prefix
				List<Integer> index = letterIndices.get(category.prefixStr);
				if (index != null) {
					// exhaustive search
					for (int pos : index) {
						CategoryNode node = childNodes.get(pos);
						if (node.categoryStr.equals(category.str)) {
							return node;
						} 
						
						if (category.str.startsWith(node.categoryPrefix)) {
							CategoryNode res = node.findParent(category.str);
							if (res != null) {
								return res;
							}
						}		
					}
				}
			} else {
				// range prefix
				List<Range> ranges = new ArrayList<Range>(rangeIndices.keySet());
				Collections.sort(ranges);
				int pos = Collections.binarySearch(ranges, category.range, new Comparator<Range>() {
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
				
				if (pos >= 0) {
					CategoryNode node = childNodes.get(rangeIndices.get(ranges.get(pos)));
					if (node.children.childNodes.size() == 0 || node.categoryStr.equals(category.str)) {
						return node;
					} 
					CategoryNode res = node.findParent(category.str);
					if (res != null) {
						return res;
					}
				} else if ((range.min >= category.range.min && range.max <= category.range.max)
					|| (category.range.min >= range.min && category.range.max <= range.max)) {
					return CategoryNode.this;
				}
			}
			
			return null;
		 }
	}

	class Category {
		String str; // the whole string
		String prefixStr = null; 
		boolean isLetter = false;
		Range range = null;
		
		public Category(String str) {		
			this.str = str;
			if (categoryPrefix != null) {
				String tmp = str.substring(categoryPrefix.length());				
				this.prefixStr = (tmp.length() == 0 || !(tmp.charAt(0) >= 'A' && tmp.charAt(0) <= 'Z')) ? 
					tmp: tmp.substring(0, 1);
				isLetter = (rangePattern.matcher(prefixStr).matches() || digitPattern
					.matcher(prefixStr).matches()) ? false : true; 
			} else {
				char c = str.charAt(0);
				if (c >= 'A' && c <= 'Z') {
					this.prefixStr = str.substring(0, 1);
					isLetter = true;
				} else {
					this.prefixStr = str;
				}
			}	
			
			if (!isLetter) {
				range = Range.valueOf(prefixStr);
				if (range == null) logger.error("Fail to parse " + prefixStr + " to range!");
			}
		}
		
		public String toString() {
			return this.str;
		}
		
		@Override
		public int hashCode() {
			return str.hashCode();
		}
		
		@Override
		public boolean equals(Object obj) {
			String cmp = ((Category) obj).str;
			return this.str.equals(cmp);
		}
		
		
//		@Override
//		public int hashCode() {
//			return (prefixStr.equals("E-F") || prefixStr.startsWith("E") || prefixStr
//				.startsWith("F")) ? "E".hashCode() : prefixStr.hashCode();
//		}
//		
//		@Override
//		public boolean equals(Object obj) {
//			String cmp = ((Prefix) obj).prefixStr;
//			if (prefixStr.equals(cmp)){
//				return true;
//			} else if (prefixStr.equals("E-F")) {
//				return (cmp.equals("E") || cmp.equals("F")) ? true : false;
//			} else if (cmp.equals("E-F")) {
//				return (prefixStr.equals("E") || (prefixStr.equals("F"))) ? true : false;
//			} else {
//				return this.prefixStr.equals(((Prefix)obj).prefixStr);
//			}
//		}
	}
	
	private Children children = new Children();
	private String categoryStr = null;
	private Range range = null;
	private String categoryPrefix = null;

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
		Matcher matcher = letterPattern.matcher(category);
		if (matcher.find()) {
			this.categoryPrefix = matcher.group(0);
			String suffix = category.substring(categoryPrefix.length());
			if (suffix.length() > 0) {
				range = Range.valueOf(suffix);
				if (range == null) logger.error("Fail to parse " + suffix + " to range for " + category);
			}
		}
	}

	public CategoryNode addChild(String str) {		
		CategoryNode child = new CategoryNode(str);
		children.addChild(new Category(str), child);
		return child;
	}

	public CategoryNode findParent(String category) {
		return children.getParent(new Category(category));
	}
	
	public void addId(String volumeID) {
		this.idList.add(volumeID);
	}

	// TODO: fix me!
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
		
		// binary search
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
