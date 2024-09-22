module com.parallelsymmetry.dalton {

	// Compile time only
	requires static lombok;

	// Compile and runtime
	requires jscience;
	requires utility;
	requires purejavacomm;
	requires com.fasterxml.jackson.core;
	requires service;
	requires java.logging;

}
