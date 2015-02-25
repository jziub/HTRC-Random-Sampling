package edu.indiana.d2i.htrc.randomsampling.tree;

import junit.framework.Assert;

import org.junit.Test;

import edu.indiana.d2i.htrc.randomsampling.Configuration;
import edu.indiana.d2i.htrc.randomsampling.exceptions.NoCategoryFound;

public class TestTree {
	
	@Test
	public void testTreeCreation() throws NoCategoryFound {
		Configuration config = Configuration.getSingleton();
		config.setString(Configuration.PropertyNames.VOLUME_CALLNO, "./eng-QH-callno");
//		config.setString(Configuration.PropertyNames.VOLUME_CALLNO, "./test.txt");
		Tree tree = Tree.getSingelton(config);
		Assert.assertEquals(tree.idCntInserted, tree.idCount());		
		
		System.out.println(tree.idCount());
		System.out.println("QH1-278.5 " + tree.findIdCount("QH1-278.5"));
		System.out.println("QH1-199.5 " + tree.findIdCount("QH1-199.5"));
		System.out.println("QH201-278.5 " + tree.findIdCount("QH201-278.5"));
		System.out.println("QH301-705.5 " + tree.findIdCount("QH301-705.5"));
		System.out.println("QH359-425 " + tree.findIdCount("QH359-425"));
		System.out.println("QH426-470 " + tree.findIdCount("QH426-470"));
		System.out.println("QH471-489 " + tree.findIdCount("QH471-489"));
		System.out.println("QH501-531 " + tree.findIdCount("QH501-531"));
		System.out.println("QH540-549.5 " + tree.findIdCount("QH540-549.5"));
		System.out.println("QH573-671 " + tree.findIdCount("QH573-671"));
		System.out.println("QH705-705.5 " + tree.findIdCount("QH705-705.5"));
		
	}
}
