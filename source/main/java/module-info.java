module com.parallelsymmetry.dalton {

	// Compile time only
	requires static lombok;

	// Compile and runtime
	requires com.fasterxml.jackson.core;
	requires com.parallelsymmetry.utility;
	requires com.parallelsymmetry.service;
	requires java.logging;
	requires jscience;
	requires purejavacomm;
}
