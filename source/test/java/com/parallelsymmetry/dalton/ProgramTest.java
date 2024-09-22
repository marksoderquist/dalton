package com.parallelsymmetry.dalton;

import com.parallelsymmetry.utility.TestUtil;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ProgramTest {

	@Test
	public void testStart() {
		assertThat( TestUtil.isTest() ).isTrue();

		Program.main( new String[ 0 ] );

		//Program program = new Program();

		//program.startAndWait();
		//program.process( new String[0] );

	}

}
