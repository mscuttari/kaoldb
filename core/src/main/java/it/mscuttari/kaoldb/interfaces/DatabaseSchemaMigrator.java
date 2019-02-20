package it.mscuttari.kaoldb.interfaces;

/**
 * Database schema migrator to be used in case of database version change
 */
public interface DatabaseSchemaMigrator {

    /**
     * Called in case of database version upgrade.
     *
     * All the data is in the dump and all the data that has to be kept must be persisted again
     * using the new version POJOs. The data not persisted again will be lost.
     *
     * Moreover, the upgrades are done incrementally, therefore in every call
     * newVersion = oldVersion + 1. For example, if the database version was 54 and now is 57,
     * this method is called three times, with the following values:
     *  -   oldVersion = 54, newVersion = 55, dump = all data of version 54
     *  -   oldVersion = 55, newVersion = 56, dump = all data of version 55
     *  -   oldVersion = 56, newVersion = 57, dump = all data of version 56
     *
     * @param oldVersion    old version
     * @param newVersion    new version
     * @param dump          database dump at version oldVersion
     */
    void onUpgrade(int oldVersion, int newVersion, DatabaseDump dump) throws Exception;


    /**
     * Called in case of database version downgrade.
     *
     * All the data is in the dump and all the data that has to be kept must be persisted again
     * using the new version POJOs. The data not persisted again will be lost.
     *
     * Moreover, the downgrades are done incrementally, therefore in every call
     * newVersion = oldVersion - 1. For example, if the database version was 57 and now is 54,
     * this method is called three times, with the following values:
     *  -   oldVersion = 57, newVersion = 56, dump = all data of version 57
     *  -   oldVersion = 56, newVersion = 55, dump = all data of version 56
     *  -   oldVersion = 55, newVersion = 54, dump = all data of version 55
     *
     * @param oldVersion    old version
     * @param newVersion    new version
     * @param dump          database dump at version oldVersion
     */
    void onDowngrade(int oldVersion, int newVersion, DatabaseDump dump) throws Exception;

}
