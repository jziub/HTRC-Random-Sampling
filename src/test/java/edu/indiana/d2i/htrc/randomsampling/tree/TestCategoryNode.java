package edu.indiana.d2i.htrc.randomsampling.tree;

import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import edu.indiana.d2i.htrc.randomsampling.exceptions.SampleNumberTooLarge;

public class TestCategoryNode extends CategoryNode {

	@Test
	public void testCategory() {
		Category category1 = new Category("QH545");
		Assert.assertEquals("Q", category1.getCurrentCategoryStr());
		Assert.assertEquals("H545", category1.getSubCategoryStr());
		
		Category category2 = new Category("301-355");
		Assert.assertNotNull(category2.range);
		Assert.assertEquals(301, category2.range.min, 0.01);
		Assert.assertEquals(355, category2.range.max, 0.01);
		Assert.assertNull(category2.getSubCategoryStr());
		
		Category category3 = new Category("410");
		Assert.assertNotNull(category3.range);
		Assert.assertEquals(410, category3.range.min, 0.01);
		Assert.assertEquals(410, category3.range.max, 0.01);
		Assert.assertNull(category3.getSubCategoryStr());
		
		Category category4 = new Category("178.34-190.25");
		Assert.assertNotNull(category4.range);
		Assert.assertEquals(178.34, category4.range.min, 0.01);
		Assert.assertEquals(190.25, category4.range.max, 0.01);
		Assert.assertNull(category4.getSubCategoryStr());
		
		Assert.assertTrue(CategoryNode.validateCategoryString("QH366"));
		Assert.assertTrue(CategoryNode.validateCategoryString("QH43.23"));
		Assert.assertFalse(CategoryNode.validateCategoryString("U56 no.82"));
	}
	
	@Test
	public void testChildren1() {
		CategoryNode root = new CategoryNode();
		root.addCategory("Q1-390");
		root.addCategory("Q1-295");
		root.addCategory("Q300-390");
		root.addCategory("Q350-390");
		
		Assert.assertEquals(1, root.childrenCount());
		Assert.assertEquals(1, root.find("Q").childrenCount());
		Assert.assertEquals(2, root.find("Q1-390").childrenCount());
		Assert.assertEquals(0, root.find("Q1-295").childrenCount());
	}
	
	@Test
	public void testChildren2() {
		CategoryNode root = new CategoryNode();
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
		
		root.addCategory("QL1-991");
		root.addCategory("QL1-355");
		root.addCategory("QL360-599.82");
		root.addCategory("QL461-599.82");
		root.addCategory("QL605-739.8");
		root.addCategory("QL614-639.8");
		root.addCategory("QL640-669.3");
		root.addCategory("QL671-699");		
		root.addCategory("QL700-739.8");
		root.addCategory("QL750-795");
		root.addCategory("QL791-795");
		root.addCategory("QL799-799.5");
		root.addCategory("QL801-950.9");
		root.addCategory("QL951-991");
		
		// check the tree structure
		Assert.assertEquals(1, root.childrenCount());
		Assert.assertEquals(2, root.find("Q").childrenCount());
		Assert.assertEquals(2, root.find("QH").childrenCount());
		Assert.assertEquals(7, root.find("QH301-705.5").childrenCount());
		Assert.assertEquals(7, root.find("QH301").childrenCount());
		Assert.assertEquals(2, root.find("QH1-278.5").childrenCount());
		Assert.assertEquals(0, root.find("QH1-199.5").childrenCount());
		
		Assert.assertEquals(1, root.find("QL").childrenCount());
		Assert.assertEquals(7, root.find("QL1-991").childrenCount());
		Assert.assertEquals(4, root.find("QL605-739.8").childrenCount());
		Assert.assertEquals(1, root.find("QL750-795").childrenCount());
		Assert.assertEquals(0, root.find("QL614-639.8").childrenCount());
		
		// check not found case
		Assert.assertNull(root.find("QK"));
		
		// check point query
		Assert.assertEquals(root.find("QH359-425"), root.find("QH360"));
		Assert.assertEquals(root.find("QH540-549.5"), root.find("QH547.3"));
	}
	
	@Test
	public void testRandomSampling() throws SampleNumberTooLarge {
		CategoryNode root = new CategoryNode();
		root.addCategory("Q1-390");
		root.addCategory("Q1-295");
		root.addCategory("Q300-390");
		root.addCategory("Q350-390");
		
		for (int i = 0; i < 10; i++) {
			CategoryNode node = root.find("Q1-390");
			node.addId("a-" + i);
		}
		
		for (int i = 0; i < 20; i++) {
			CategoryNode node = root.find("Q1-295");
			node.addId("b-" + i);
		}
		
		for (int i = 0; i < 5; i++) {
			CategoryNode node = root.find("Q300-390");
			node.addId("c-" + i);
		}
		
		for (int i = 0; i < 5; i++) {
			CategoryNode node = root.find("Q350-390");
			node.addId("d-" + i);
		}
		
		List<String> samples1 = root.find("Q1-390").samples(4);
		Collections.sort(samples1);
		System.out.println(samples1.toString());
	}
}
