import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

// Not much to comment in this class. Mostly "eating" according to the Jack language grammar.
public class CompilationEngine {
	private File file;
	private Token currentToken;
	private JackTokenizer tokenizer;
	private boolean state = true, advanced, isConstructor = true;
	private ArrayList<String> eatArguments = new ArrayList<>();
	private SymbolTable symbolTable;
	private VMWriter vmWriter;
	private String className, currentSubroutine, termStart;
	private int expressionArgs, whileLabelIndex = 0, ifLabelIndex = 0;
	private char currentSymbol;

	CompilationEngine(File fin, File fout, SymbolTable st) throws FileNotFoundException{
		file = fin;
		vmWriter = new VMWriter(fout);
		tokenizer = new JackTokenizer(file);
		symbolTable = st;
	}
	
	//"eating" according to the Jack language grammar.
	void compileClass() throws IOException{
		advance();
		encapsulateEat("class");
		encapsulateEat("Identifier");//className
		className = tokenizer.identifier();
		encapsulateEat("{");
		while(advanced && (currentToken == Token.KEYWORD && (tokenizer.keyword().toString().toLowerCase().equals("static") || tokenizer.keyword().toString().toLowerCase().equals("field")))) {
			compileClassVarDec();
		}
		while(tokenizer.hasMoreTokens()) {
			compileSubroutine();
		}
		encapsulateEat("}");
	}
	
	void compileClassVarDec() throws IOException{
		Kind kind = null;
        if(tokenizer.keyword().toString().toLowerCase().equals("static")) {
        	kind = Kind.STATIC;
        }
        else if(tokenizer.keyword().toString().toLowerCase().equals("field")) {
        	kind = Kind.FIELD;
        }
        else {
        	System.out.println("Error, var is neither static nor field");
        }
		encapsulateEat("static::field");
        String type = getType();
		encapsulateEat("int::char::boolean::Identifier");//className
		String name = tokenizer.identifier();
		symbolTable.define(name,type,kind);
		encapsulateEat("Identifier");//varName
		while(advanced) {
			encapsulateEat(",");
			name = tokenizer.identifier();
			if(advanced) {
				symbolTable.define(name,type,kind);
			}
			encapsulateEat("Identifier");//varName
		}
		encapsulateEat(";");
	}

	void compileSubroutine() throws IOException{
		symbolTable.startSubroutine();
		whileLabelIndex = 0;
		ifLabelIndex = 0;
		isConstructor = false;
		Keyword keyword = tokenizer.keyword();
	    if (keyword == Keyword.METHOD){
	        symbolTable.define("this",className, Kind.ARG);
	    }
		encapsulateEat("constructor::function::method");
		encapsulateEat("void::int::char::boolean::Identifier");//className
		currentSubroutine = tokenizer.identifier();
		encapsulateEat("Identifier");//subroutineName
        encapsulateEat("(");
		compileParameterList();
		encapsulateEat(")");
		encapsulateEat("{");
		while(advanced && (currentToken == Token.KEYWORD && tokenizer.keyword().toString().toLowerCase().equals("var"))) {
			compileVarDec();
		}
		vmWriter.writeFunction(className + "." + currentSubroutine,symbolTable.varCount(Kind.VAR));
		if (keyword == Keyword.METHOD){
			vmWriter.writePush(Segment.ARG, 0);
			vmWriter.writePop(Segment.POINTER,0);
		}
		else if (keyword == Keyword.CONSTRUCTOR){
			vmWriter.writePush(Segment.CONST,symbolTable.varCount(Kind.FIELD));
			vmWriter.writeCall("Memory.alloc", 1);
			vmWriter.writePop(Segment.POINTER,0);
			isConstructor = true;
		}
		compileStatements();
		encapsulateEat("}");
	}

	void compileParameterList() throws IOException{
		if(advanced) {
			String type = getType();
			encapsulateEat("int::char::boolean::Identifier");//className
			symbolTable.define(tokenizer.identifier(),type, Kind.ARG);
			encapsulateEat("Identifier");//varName
			while(advanced) {
				encapsulateEat(",");
				type = getType();
				encapsulateEat("int::char::boolean::Identifier");//className
				if(advanced) {
					symbolTable.define(tokenizer.identifier(),type, Kind.ARG);
				}
				encapsulateEat("Identifier");//varName
			}
		}
	}
	
	void compileVarDec() throws IOException{
		encapsulateEat("var");
        String type = getType();
		encapsulateEat("int::char::boolean::Identifier");//className
        symbolTable.define(tokenizer.identifier(),type, Kind.VAR);
		encapsulateEat("Identifier");//varName
		while(advanced) {
			encapsulateEat(",");
			if(advanced) {
				symbolTable.define(tokenizer.identifier(),type, Kind.VAR);
			}
			encapsulateEat("Identifier");//varName
		}
		encapsulateEat(";");
	}
	
	void compileStatements() throws IOException{
		advanced = true;
		while(advanced) {
			advanced=false;
			state = false;
			
			encapsulateEat("do");
			if(advanced) {
				compileDo();
				continue;
			}
			else {
				advanced = false;
			}
			
			encapsulateEat("let");
			if(advanced) {
				compileLet();
				continue;
			}
			else {
				advanced = false;
			}
			
			encapsulateEat("if");
			if(advanced) {
				compileIf();
				continue;
			}
			else {
				advanced = false;
			}
			
			encapsulateEat("while");
			if(advanced) {
				compileWhile();
				continue;
			}
			else {
				advanced = false;
			}
			
			encapsulateEat("return");
			if(advanced) {
				compileReturn();
				continue;
			}
			else {
				advanced = false;
			}
		}
		
		state = true;
	}
	
	void compileDo() throws IOException{
		state = true;
		encapsulateEat("do");
		String name = tokenizer.identifier();
        int nArgs = 0;
		encapsulateEat("Identifier");//subroutineName
		encapsulateEat("(");
		if(advanced) {
			vmWriter.writePush(Segment.POINTER,0);
			compileExpressionList();
            nArgs = expressionArgs;
		}
		encapsulateEat(")");
		if(advanced) {
	        vmWriter.writeCall(className + '.' + name, nArgs + 1);
			encapsulateEat(";");
			vmWriter.writePop(Segment.TEMP,0);
			return;
		}
		String className = tokenizer.identifier();
		encapsulateEat("Identifier");//Classname or varName
		encapsulateEat(".");
		String object = name;
		name = tokenizer.identifier();
        String type = symbolTable.typeOf(object);
		encapsulateEat("Identifier");//subroutineName
		encapsulateEat("(");
		if(advanced) {
			if(!(type.equals(""))) {
				nArgs = 1;
	            vmWriter.writePush(symbolTable.segmentOf(object), symbolTable.indexOf(object));
	            name = symbolTable.typeOf(object) + "." + name;
			}
			compileExpressionList();
			nArgs += expressionArgs;
			if(className.charAt(0) > 96 || className.contains(".")) {
				vmWriter.writeCall(name, nArgs);
			}
			else {
				vmWriter.writeCall(className + '.' + name, nArgs);
			}
		 }
		 encapsulateEat(")");
		 encapsulateEat(";");
		 vmWriter.writePop(Segment.TEMP,0);
	}
	
	void compileLet() throws IOException{
		state = true;
		encapsulateEat("let");
		String variable = tokenizer.identifier();
		boolean isArr = false;
		encapsulateEat("Identifier");//varName
		encapsulateEat("[");
		if(advanced) {
			isArr = true;
            vmWriter.writePush(symbolTable.segmentOf(variable),symbolTable.indexOf(variable));
			compileExpression();
			encapsulateEat("]");
			vmWriter.writeArithmetic(Command.ADD);
		}
		encapsulateEat("=");
		compileExpression();
		encapsulateEat(";");
		if (isArr){
            vmWriter.writePop(Segment.TEMP,0);
            vmWriter.writePop(Segment.POINTER,1);
            vmWriter.writePush(Segment.TEMP,0);
            vmWriter.writePop(Segment.THAT,0);
        }
		else {
            vmWriter.writePop(symbolTable.segmentOf(variable), symbolTable.indexOf(variable));
        }
	}
	
	void compileWhile() throws IOException{
		String whileEnd = "WHILE_END" + whileLabelIndex;
        String whileExp = "WHILE_EXP" + whileLabelIndex;
        whileLabelIndex++;
        vmWriter.writeLabel(whileExp);
		state = true;
		encapsulateEat("while");
		encapsulateEat("(");
		compileExpression();
		encapsulateEat(")");
        vmWriter.writeArithmetic(Command.NOT);
        vmWriter.writeIf(whileEnd);
		encapsulateEat("{");
		compileStatements();
		encapsulateEat("}");
        vmWriter.writeGoto(whileExp);
        vmWriter.writeLabel(whileEnd);
	}
	
	void compileReturn() throws IOException{
		state = true;
		encapsulateEat("return");
		// checking if the next token is ';' which means that there are no expressions.
		if(!(currentToken == Token.SYMBOL && tokenizer.symbol() == ';')) {
			compileExpression();
		}
		else {
			if(isConstructor) {
				vmWriter.writePush(Segment.POINTER,0);
			}
			else {
				vmWriter.writePush(Segment.CONST,0);
			}
		}
		encapsulateEat(";");
		vmWriter.writeReturn();
	}
	
	void compileIf() throws IOException{
		String ifLabel = "IF_TRUE" + ifLabelIndex;
		String elseLabel = "IF_FALSE" + ifLabelIndex;
        String endLabel = "IF_END" + ifLabelIndex;
        ifLabelIndex++;
		state = true;
		encapsulateEat("if");
		encapsulateEat("(");
		compileExpression();
		encapsulateEat(")");
		vmWriter.writeIf(ifLabel);
        vmWriter.writeGoto(elseLabel);
        vmWriter.writeLabel(ifLabel);
		encapsulateEat("{");
		compileStatements();
		encapsulateEat("}");
		if(advanced) {
			encapsulateEat("else");
			if(advanced) {
				vmWriter.writeGoto(endLabel);
		        vmWriter.writeLabel(elseLabel);
				encapsulateEat("{");
				compileStatements();
				encapsulateEat("}");
				vmWriter.writeLabel(endLabel);
			}
			else {
				vmWriter.writeLabel(elseLabel);
			}
			advanced = true;
		}
	}
	
	void compileExpression() throws IOException{
		compileTerm();
		//more than one term
		 do{   
			encapsulateEat("+::-::*::/::&::|::<::>::=");
			if(advanced) {
				Command com = Command.NOT;
				String func = "";
	            switch (currentSymbol){
	                case '+':com = Command.ADD;break;
	                case '-':com = Command.SUB;break;
	                case '<':com = Command.LT;break;
	                case '>':com = Command.GT;break;
	                case '=':com = Command.EQ;break;
	                case '&':com = Command.AND;break;
	                case '|':com = Command.OR;break;
	                case '*':func = "Math.multiply";break;
	                case '/':func = "Math.divide";break;
	                default:;break;
	            }
				compileTerm();
				if(com == Command.NOT) {
					vmWriter.writeCall(func, 2);
				}
				else {
					vmWriter.writeArithmetic(com);
				}
			}
		 }while(advanced);
		 if(currentToken == Token.SYMBOL && tokenizer.symbol() == ',') {
				advanced = true;
		 }
	}
	
	void compileTerm() throws IOException{
		termStart = tokenizer.identifier();
		String currTermStart = tokenizer.identifier();
		char unaryOp = tokenizer.symbol();
		
		// if unaryOp term 
		encapsulateEat("-::~");
		if(advanced){
			compileTerm();
			if (unaryOp == '-'){
                vmWriter.writeArithmetic(Command.NEG);
            }else {
                vmWriter.writeArithmetic(Command.NOT);
            }
			return;
		}
		
		if(tokenizer.tokenType() == Token.KEYWORD && tokenizer.keyword() == Keyword.TRUE){
            vmWriter.writePush(Segment.CONST,0);
            vmWriter.writeArithmetic(Command.NOT);
            advance();
            return;
        }
		else if(tokenizer.tokenType() == Token.KEYWORD && tokenizer.keyword() == Keyword.THIS){
            vmWriter.writePush(Segment.POINTER,0);
            advance();
            return;
        }
		else if(tokenizer.tokenType() == Token.KEYWORD && (tokenizer.keyword() == Keyword.FALSE || tokenizer.keyword() == Keyword.NULL)){
            vmWriter.writePush(Segment.CONST,0);
            advance();
            return;
        }
		
		//if '(' expression ')' 
		encapsulateEat("(");
		if(advanced) {
			compileExpression();
			encapsulateEat(")");
			if(advanced) { 
				return;
			}
		}
		
		//if subroutineCall 
		encapsulateEat("Identifier"); //subroutineName
		encapsulateEat("(");
		if(advanced) {
			 compileExpressionList();
			 encapsulateEat(")");
				if(advanced) {
					encapsulateEat(";");
					return;
				}
		 }
		 encapsulateEat("Identifier"); //Classname or varName
		 encapsulateEat(".");
		 String name = tokenizer.identifier();
		 encapsulateEat("Identifier"); //subroutineName
		 encapsulateEat("(");
		 if(advanced) {
			 String type = symbolTable.typeOf(termStart);
			 if(type.equals("")) {
				 name = termStart + "." + name;
			 }
			 else {
				 expressionArgs = 1;
	             vmWriter.writePush(symbolTable.segmentOf(termStart), symbolTable.indexOf(termStart));
	             name = symbolTable.typeOf(termStart) + "." + name;
	         }
			 compileExpressionList();
			 encapsulateEat(")");
			 if(advanced) { 
				 if(currTermStart.charAt(0)>96) {
					 vmWriter.writeCall(name, expressionArgs + 1);
				 }
				 else {
					 vmWriter.writeCall(name, expressionArgs);
				 }
				 return;
			 }
		 }
		 
		 // if varName '[' expression']' 
		 encapsulateEat("Identifier"); //varName
		 encapsulateEat("[");
		 if(advanced) {
			 String ctStart = termStart;
			 compileExpression();
			 vmWriter.writePush(symbolTable.segmentOf(ctStart),symbolTable.indexOf(ctStart));
			 encapsulateEat("]");
			 if(advanced) { 
                vmWriter.writeArithmetic(Command.ADD);
                vmWriter.writePop(Segment.POINTER,1);
                vmWriter.writePush(Segment.THAT,0);
				return;
			}
		}
		
		encapsulateEat("integerConstant::stringCostant::Keyword");
		if(!advanced) { // if varName 
			vmWriter.writePush(symbolTable.segmentOf(termStart), symbolTable.indexOf(termStart));
			return;
		}
	}
	
	void compileExpressionList() throws IOException{
		expressionArgs = 0;
		// checking if the next token is ')' which means that there are no expressions, so returning instead
		if(currentToken == Token.SYMBOL && tokenizer.symbol() == ')') {
			return;
		}
		if(advanced) {
			compileExpression();
			expressionArgs++;
			while(advanced && !(currentToken == Token.SYMBOL && tokenizer.symbol() == ')')) {
				encapsulateEat(",");
				compileExpression();
				expressionArgs++;
			}
		}
	}
	
	// Helper functions
	
	private void eat(ArrayList<String> arguments) throws IOException {
		if(currentToken != null) {
			switch (currentToken) {
			case KEYWORD:
				if(!arguments.contains(tokenizer.keyword().toString().toLowerCase()) && !arguments.contains("Keyword")) {
					//The argument is not correct.
					advanced = false;
				}
				else if (!state) {
					//The argument is correct! NOT writing to file and NOT advancing, just accepting.
					advanced = true;
				}
				else {
					//The argument is correct! writing to file and advancing.
					advanced = true;
					advance();
					if(tokenizer.keyword() == Keyword.TRUE) {
						vmWriter.writePush(Segment.CONST,0);
		                vmWriter.writeArithmetic(Command.NOT);
					}
					else if(tokenizer.keyword() == Keyword.FALSE || tokenizer.keyword() == Keyword.NULL) {
						vmWriter.writePush(Segment.CONST,0);
					}
				}
				break;
			case SYMBOL:
				if(!arguments.contains(tokenizer.symbol() + "") && !arguments.contains("Symbol")) {
					advanced = false;
				}
				else if (!state) {
					advanced = true;
				}
				else {
					currentSymbol = tokenizer.symbol();
					advanced = true;
					advance();
				}
				break;
			case IDENTIFIER:
				if(!arguments.contains(tokenizer.identifier()) && !arguments.contains("Identifier")) {
					advanced = false;
				}
				else if (!state) {
					advanced = true;
				}
				else {
					advanced = true;
					advance();
				}
				break;
			case INT_CONST:
				if(!arguments.contains(String.valueOf(tokenizer.intVal())) && !arguments.contains("integerConstant")) {
					advanced = false;
				}
				else if (!state) {
					advanced = true;
				}
				else {
					advanced = true;
					advance();
					vmWriter.writePush(Segment.CONST,tokenizer.intVal());
				}
				break;
			case STRING_CONST:
				if(!arguments.contains(tokenizer.stringVal()) && !arguments.contains("stringCostant")) {
					advanced = false;
				}
				else if (!state) {
					advanced = true;
				}
				else {
					advanced = true;
					advance();
					String stringConstant = tokenizer.stringVal();
	                vmWriter.writePush(Segment.CONST,stringConstant.length()-2);
	                vmWriter.writeCall("String.new",1);
	                for (int i = 0; i < stringConstant.length(); i++){
	                	if(stringConstant.charAt(i)!='\"') {
	                		vmWriter.writePush(Segment.CONST,(int)stringConstant.charAt(i));
		                    vmWriter.writeCall("String.appendChar",2);
	                	}
	                }
				}
				break;
			default:
				System.out.println("Unknown token");
				break;
			}
		}
		else {
			System.out.println("The request is null, so not doing anything.");
		}
		
	}

	private String getType(){
        if (tokenizer.tokenType() == Token.KEYWORD && (tokenizer.keyword() == Keyword.BOOLEAN || tokenizer.keyword() == Keyword.INT || tokenizer.keyword() == Keyword.CHAR)){
            return tokenizer.keyword().toString().toLowerCase();
        }
        else if (tokenizer.tokenType() == Token.IDENTIFIER){
            return tokenizer.identifier();
        }
        return "";
    }
	
	private void advance() {
		if(tokenizer.hasMoreTokens()) {
			tokenizer.advance();
			currentToken = tokenizer.tokenType();
		}
	}
	
	private void encapsulateEat(String argumentsString) throws IOException{
		String[] arguments = argumentsString.split("::");
		for(String arg : arguments) {
			eatArguments.add(arg);
		}
		eat(eatArguments);
		eatArguments.clear();
	}
	
	public void close() throws IOException{
		vmWriter.close();
	}
}