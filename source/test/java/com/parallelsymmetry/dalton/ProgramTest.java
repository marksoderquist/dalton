package com.parallelsymmetry.dalton;

import com.parallelsymmetry.utility.TestUtil;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class ProgramTest {

	@Test
	public void testStart() throws Exception {
		assertThat( TestUtil.isTest(), is( true ) );

		Program.main( new String[0] );

		//Program program = new Program();

		//program.startAndWait();
		//program.process( new String[0] );


	}

}
