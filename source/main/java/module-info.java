module com.parallelsymmetry.dalton {

	// Compile time only
	requires static lombok;

	// Compile and runtime
	requires jscience;
	requires purejavacomm;
	requires com.fasterxml.jackson.core;
	requires java.logging;
	requires com.parallelsymmetry.utility;
	requires com.parallelsymmetry.service;
}
