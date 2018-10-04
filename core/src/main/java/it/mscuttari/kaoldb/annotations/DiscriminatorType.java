package it.mscuttari.kaoldb.annotations;

public enum DiscriminatorType {
    STRING,
    CHAR,
    INTEGER;

    DiscriminatorType() {

    }


    /**
     * Get corresponding discriminator column class
     *
     * @return  column class
     */
    public Class<?> getDiscriminatorClass() {
        switch (this) {
            case STRING:
                return String.class;

            case INTEGER:
                return Integer.class;

            default:
                return String.class;
        }
    }

}