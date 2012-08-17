package org.rdfhdt.hdt.dictionary.impl;

import java.io.File;

import org.rdfhdt.hdt.hdt.HDTVocabulary;
import org.rdfhdt.hdt.options.HDTSpecification;

import com.sleepycat.db.DatabaseException;
import com.sleepycat.db.Environment;
import com.sleepycat.db.EnvironmentConfig;

public class BerkeleyDBDictionary_native extends BaseModifiableDictionary {

	private Environment env;

	public BerkeleyDBDictionary_native(HDTSpecification spec) {

		super(spec);
		setupDBEnvironment(spec);

		// FIXME: Read stuff from properties
		try {
			subjects = new DictionarySectionBerkeley_native(spec, env, "subjects");
			predicates = new DictionarySectionBerkeley_native(spec, env, "predicates");
			objects = new DictionarySectionBerkeley_native(spec, env, "objects");
			shared = new DictionarySectionBerkeley_native(spec, env, "shared");
		} catch (Exception e){
			//TODO something smarter??
			cleanupEnvironment();
			throw new RuntimeException(e);
		}
	}

	private void setupDBEnvironment(HDTSpecification spec) {

		//FIXME read from specs...
		File folder = new File("DB");
		if (!folder.exists() && !folder.mkdir()){
			throw new RuntimeException("Unable to create DB folder...");
		}

		EnvironmentConfig envConf = new EnvironmentConfig();
		envConf.setAllowCreate(true);
		envConf.setTransactional(false);
		envConf.setTemporaryDirectory(new File("DB"));
		envConf.setInitializeCache(true);
		envConf.setCacheSize(calculateCache(spec));
		
		try {
			env = new Environment(folder, envConf);
		} catch (Exception e) {
			throw new RuntimeException("Unable to open environment.", e);
		}
	}
	
	/**
	 * Takes a parcent of the maximum possible heap size for this runtime and returns
	 * it's nearest power of 2.
	 * 
	 * TODO could it be done a bit smarter?... maybe prefering the bigger power a bit...? 
	 */
	private long calculateCache(HDTSpecification spec){
		
		long Xmx = Runtime.getRuntime().maxMemory();
		if (Xmx==Long.MAX_VALUE)
			throw new RuntimeException("Unable to calculate cache because max heap size not specified. Please set Xmx parameter.");
		
		double percent = 0.25; //TODO read from specs... ??
		int closestPowerOf2 = Math.round((float)(Math.log(Xmx*percent)/Math.log(2)));
		
		return (long)Math.pow(2, closestPowerOf2);
	}

	@Override
	public void startProcessing() {
		//do nothing
	}

	@Override
	public void endProcessing() {
		//do nothing
	}
	
	/**
	 * Closes the environment and cleans up the environment home folder.
	 */
	private void cleanupEnvironment() {
		try {
			File envHome = env.getHome();
			env.close();
			env = null;

			//TODO cleanup DB folder manually like this, or not? ??
			for (File f : envHome.listFiles()){
				String fname = f.getName();
				if (fname.equalsIgnoreCase("je.properties"))
					continue;
				if (fname.startsWith("__db"))
					f.delete();
			}

		} catch (DatabaseException dbExc){
			throw new RuntimeException("Unable to close environment (most probably files left behind in the environment home folder)", dbExc);
		}
	}

	@Override
	public void close() {

		try {	
			((DictionarySectionBerkeley_native)subjects).cleanup();
			((DictionarySectionBerkeley_native)predicates).cleanup();
			((DictionarySectionBerkeley_native)objects).cleanup();
			((DictionarySectionBerkeley_native)shared).cleanup();
		} catch (DatabaseException e){
			cleanupEnvironment();
			throw new RuntimeException("Closing of databases failed (most probably files left behind)", e);
		}
		
		if (env!=null) //in some wierd case the above cleanupEnvironment was called and the execution still came here...
			cleanupEnvironment();

	}

	/* (non-Javadoc)
	 * @see hdt.dictionary.Dictionary#getType()
	 */
	@Override
	public String getType() {
		//FIXME ... different type?
		return HDTVocabulary.DICTIONARY_TYPE_PLAIN;
	}
}
