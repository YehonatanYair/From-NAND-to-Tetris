import java.util.HashMap;

//Not much to comment in this class. implementing given methods in a simple manner with if/else or switch cases.
public class SymbolTable {
  	private HashMap<String,Element> classST;
    private HashMap<String,Element> subroutineST;
    int argN = 0, fieldN = 0, staticN = 0, varN = 0;

    public SymbolTable() {
    	classST = new HashMap<String, Element>();
    	subroutineST = new HashMap<String, Element>();
    }

    public void startSubroutine(){
    	subroutineST.clear();
    	varN = 0;
    	argN = 0;
    }

    public void define(String name, String type, Kind kind){
    	Element elm;
        switch (kind){
            case ARG:
            	elm = new Element(type,kind,argN);
                argN++;
                subroutineST.put(name,elm);
                break;
            case VAR:
            	elm = new Element(type,kind,varN);
            	varN++;
            	subroutineST.put(name,elm);
            	break;
            case STATIC:
            	elm = new Element(type,kind,staticN);
            	staticN++;
            	classST.put(name,elm);
                break;
            case FIELD:
            	elm = new Element(type,kind,fieldN);
            	fieldN++;
            	classST.put(name,elm);
                break;
            default:
            	System.out.println("Error in define: unrecognized kind");
            	break;
        }
    }

    public int varCount(Kind kind){
    	switch (kind){
        case ARG:
            return argN;
        case VAR:
        	return varN;
        case STATIC:
        	return staticN;
        case FIELD:
        	return fieldN;
        default:
        	System.out.println("Error in varCount: unrecognized kind");
        	return 0;
    	}
    }

    public Kind kindOf(String name){
    	if(classST.get(name) != null){
            return classST.get(name).kind;
        }
    	else{ //subroutineST.get(name)
            return subroutineST.get(name).kind;
        }
    }

    public String typeOf(String name){
    	if(classST.get(name) != null){
            return classST.get(name).type;
        }
    	else if(subroutineST.get(name) != null) {
            return subroutineST.get(name).type;
        }
    	else {
    		return "";
    	}
    }

    public int indexOf(String name){
    	if(classST.get(name) != null){
            return classST.get(name).index;
        }
    	else{
            return subroutineST.get(name).index;
        }
    }
    
    public Segment segmentOf(String name){
    	Kind kind = kindOf(name);
        switch (kind){
            case FIELD:return Segment.THIS;
            case STATIC:return Segment.STATIC;
            case VAR:return Segment.LOCAL;
            case ARG:return Segment.ARG;
            default:
            	System.out.println("Error in segmentOf: unrecognized kind");
            	return null;
        }
    }
    
    // inner class to encapsulate type, kind and index all together.
    class Element {
        protected String type;
        protected Kind kind;
        protected int index;

        public Element(String s, Kind k, int i) {
            type = s;
            kind = k;
            index = i;
        }
    }
}


	