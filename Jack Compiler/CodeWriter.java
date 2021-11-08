package vmtranslator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Stack;

public class CodeWriter {
	
	private BufferedWriter writer;
	private String fileName;
	private int labelIndex;
	private int returnIndex;
	private Stack<String> functionNames = new Stack<String>();
	
	public CodeWriter(File outputFile) throws IOException{
		writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile)));
	}
	
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	
	public void writeArithmetic(String command) throws IOException{
		debug();
		writer.write("@SP\nA=M-1\n"); 
		switch (command) { // true == -1, false == 0
		case "add":
			writer.write("D=M\n@SP\nM=M-1\n@SP\nA=M-1\nD=D+M\nM=D\n");
			break;
		case "sub":
			writer.write("D=-M\n@SP\nM=M-1\n@SP\nA=M-1\nD=D+M\nM=D\n");
			break;
		case "neg":
			writer.write("M=-M\n");
			break;
		case "eq":
			writer.write("D=M\n@SP\nM=M-1\n@SP\nA=M-1\nD=D-M\n@TRUE" + labelIndex + "\nD;JEQ\nD=0\n@END" + labelIndex + "\n0;JMP\n(TRUE" + labelIndex + ")\nD=-1\n(END" + labelIndex + ")\n@SP\nA=M-1\nM=D\n");
			labelIndex++;
			break;
		case "gt":
			writer.write("D=M\n@SP\nM=M-1\n@SP\nA=M-1\nD=D-M\n@TRUE" + labelIndex + "\nD;JLT\nD=0\n@END" + labelIndex + "\n0;JMP\n(TRUE" + labelIndex + ")\nD=-1\n(END" + labelIndex + ")\n@SP\nA=M-1\nM=D\n");
			labelIndex++;
			break;
		case "lt":
			writer.write("D=M\n@SP\nM=M-1\n@SP\nA=M-1\nD=D-M\n@TRUE" + labelIndex + "\nD;JGT\nD=0\n@END" + labelIndex + "\n0;JMP\n(TRUE" + labelIndex + ")\nD=-1\n(END" + labelIndex + ")\n@SP\nA=M-1\nM=D\n");
			labelIndex++;
			break;
		case "and":
			writer.write("D=M\n@SP\nM=M-1\n@SP\nA=M-1\nD=D&M\nM=D\n");
			break;
		case "or":
			writer.write("D=M\n@SP\nM=M-1\n@SP\nA=M-1\nD=D|M\nM=D\n");
			break;
		default: //case not:
			writer.write("M=!M\n");
			break;
		}
	}
	
	public void writePushPop(Command command, String segment, int index) throws IOException{
		debug();
		switch (segment) {
		case "local":
			writer.write("@LCL\nD=M\n@" + index +"\nD=D+A\n");
			break;
		case "argument":
			writer.write("@ARG\nD=M\n@" + index +"\nD=D+A\n");
			break;
		case "this":
			writer.write("@THIS\nD=M\n@" + index +"\nD=D+A\n");
			break;
		case "that":
			writer.write("@THAT\nD=M\n@" + index +"\nD=D+A\n");
			break;
		case "constant":
			writer.write("@" + index +"\nD=A\n");
			writer.write("@SP\nA=M\nM=D\n@SP\nM=M+1\n");
			return;
		case "static":
			writer.write("@16\nD=A\n@" + fileName + index +"\nD=D+A\n");
			break;
		case "pointer":
			if(index == 0) {
				if(command == Command.C_PUSH) {
					writer.write("@THIS\nD=M\n@SP\nA=M\nM=D\n@SP\nM=M+1\n");
				}
				else {
					writer.write("@SP\nM=M-1\n@SP\nA=M\nD=M\n@THIS\nM=D\n");
				}
				return;
			}
			else {// index == 1
				if(command == Command.C_PUSH) {
					writer.write("@THAT\nD=M\n@SP\nA=M\nM=D\n@SP\nM=M+1\n");
				}
				else {
					writer.write("@SP\nM=M-1\n@SP\nA=M\nD=M\n@THAT\nM=D\n");
				}
				return;
			}
		default: //case temp:
			writer.write("@5\nD=A\n@" + index +"\nD=D+A\n");
			break;
		}
		if(command == Command.C_PUSH) {
			writer.write("A=D\nD=M\n@SP\nA=M\nM=D\n@SP\nM=M+1\n");
		}
		else { //command == Command.C_POP
			writer.write("@R13\nM=D\n@SP\nM=M-1\n@SP\nA=M\nD=M\n@R13\nA=M\nM=D\n");
		}
	}
	
	
	
	public void writeInit() throws IOException {
		//debug();
		writer.write("@256\nD=A\n@SP\nM=D\n");
        writeCall("Sys.init", 0);
	}
	
	
	public void writeLabel(String label) throws IOException {
		debug();
		//the if else statements in the following few functions are for making vm files which do not contain functions
		//like the ones from project 7, to still be translated successfully in this implementation of the VM translator
		if(!functionNames.empty()) {
			writer.write("(" + functionNames.peek() + '$' + label + ")\n");
		}
		else {
			writer.write("(" + label + ")\n");
		}
	}

	public void writeGoto(String label) throws IOException {
		debug();
		if(!functionNames.empty()) {
			writer.write("@" + functionNames.peek() + '$' + label + "\n0;JMP\n");
		}
		else {
			writer.write("@" + label + "\n0;JMP\n");
		}
	}

	public void writeIf(String label) throws IOException {
		debug();
		if(!functionNames.empty()) {
			writer.write("@SP\nM=M-1\nA=M\nD=M\n@" + functionNames.peek() + '$' + label + "\nD;JNE\n");
		}
		else {
			writer.write("@SP\nM=M-1\nA=M\nD=M\n@" + label + "\nD;JNE\n");
		}
	}
	
	public void writeFunction(String functionName, int numVars) throws IOException {
		debug();
		if(!functionNames.empty()) {
			functionNames.pop();
		}
		functionNames.push(functionName);
		writer.write("(" + functionName + ")\n");
		for(int i = 0; i < numVars; i++) {
			writePushPop(Command.C_PUSH,"constant", 0);
		}
		
	}
	
	public void writeCall(String functionName, int numArgs) throws IOException {
		debug();
		writer.write("@" + functionName + "$ret." + returnIndex + "\nD=A\n@SP\nA=M\nM=D\n@SP\nM=M+1\n");
		writer.write("@LCL\nD=M\n@SP\nA=M\nM=D\n@SP\nM=M+1\n");
		writer.write("@ARG\nD=M\n@SP\nA=M\nM=D\n@SP\nM=M+1\n"); 
		writer.write("@THIS\nD=M\n@SP\nA=M\nM=D\n@SP\nM=M+1\n"); 
		writer.write("@THAT\nD=M\n@SP\nA=M\nM=D\n@SP\nM=M+1\n"); 
		writer.write("@SP\nD=M\n@5\nD=D-A\n@" + numArgs + "\nD=D-A\n"); 
		writer.write("@ARG\nM=D\n@SP\nD=M\n@LCL\nM=D\n");  
		writer.write("@" + functionName + "\n0;JMP\n");
		writer.write("(" + functionName + "$ret." + returnIndex + ")\n");
		returnIndex++;
	}
	

	
	public void writeReturn() throws IOException {
		writer.write("@LCL\nD=M\n@5\nA=D-A\nD=M\n@RET\nM=D\n");
		writer.write("@SP\nA=M-1\nD=M\n@ARG\nA=M\nM=D\n");  // pushing result
		writer.write("@ARG\nA=M+1\nD=A\n@SP\nM=D\n");  //setting SP
		writer.write("@LCL\nD=M-1\n");
		writer.write("A=D\nD=M\n@THAT\nM=D\n"); //reinstating state
		writer.write("@LCL\nD=M\n@2\n");
		writer.write("A=D-A\nD=M\n@THIS\nM=D\n");
		writer.write("@LCL\nD=M\n@3\n");
		writer.write("A=D-A\nD=M\n@ARG\nM=D\n");
		writer.write("@LCL\nD=M\n@4\n");
		writer.write("A=D-A\nD=M\n@LCL\nM=D\n");
		writer.write("@RET\nA=M\n0;JMP\n");
	}
	
	public void close() throws IOException{
		writer.flush();
		writer.close();
	}
	
	//helper function that prints the current line as a comment into the file to help debugging
	private void debug() throws IOException{
		writer.write("//" + Parser.currentLine + "\n");
	}

}
