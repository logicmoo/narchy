package alice.tuprologx.ide;

public interface FontDimensionHandler {

    /**
     * Increment the font dimension of the IDE's editor 
     */
    void incFontDimension();

    /**
     * Increment the font dimension of the IDE's editor 
     */
    void decFontDimension();

    void setFontDimension(int dimension);

    int getFontDimension();

    
}
