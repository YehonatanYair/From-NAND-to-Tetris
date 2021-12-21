package vmtranslator;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parser {
	
	public static String currentLine;
	
	private Scanner reader;
	private String argument1;
	private int argument2;
	
	Parser(File file) throws FileNotFoundException{
		reader = new Scanner(file);
	}
	
	boolean hasMoreCommands(){
		return reader.hasNextLine();
	}
	
	void advance() {
		currentLine = reader.nextLine();
	}
	
	Command commandType(){
		Pattern p = Pattern.compile("(//)?([^ ]*) ?([^ ]*)? ?([^ ]*)?");
		Matcher m = p.matcher(currentLine);
		m.find();
		//if the line is empty or contains "//" at the beginning, skipping it
		if(currentLine.length() == 0 || m.group(1) != null) {
			return null;
		}
		String commandPart = m.group(2);
		Command commandType;
		switch (commandPart) {
		case "push":
			commandType = Command.C_PUSH;
			break;
		case "pop":
			commandType = Command.C_POP;
			break;
		case "add":
			commandType = Command.C_ARITHMETIC;
			break;
		case "sub":
			commandType = Command.C_ARITHMETIC;
			break;
		case "neg":
			commandType = Command.C_ARITHMETIC;
			break;
		case "eq":
			commandType = Command.C_ARITHMETIC;
			break;
		case "gt":
			commandType = Command.C_ARITHMETIC;
			break;
		case "lt":
			commandType = Command.C_ARITHMETIC;
			break;
		case "and":
			commandType = Command.C_ARITHMETIC;
			break;
		case "or":
			commandType = Command.C_ARITHMETIC;
			break;
		case "not":
			commandType = Command.C_ARITHMETIC;
			break;
		case "label":
			commandType = Command.C_LABEL;
			break;
		case "goto":
			commandType = Command.C_GOTO;
			break;
		case "if-goto":
			commandType = Command.C_IF;
			break;
		case "function":
			commandType = Command.C_FUNCTION;
			break;
		case "call":
			commandType = Command.C_CALL;
			break;
		default: //case "return":
			commandType = Command.C_RETURN;
			break;
		}
		if(commandType == Command.C_ARITHMETIC || commandType == Command.C_RETURN) {
			argument1 = m.group(2);
		}
		else {
			argument1 = m.group(3);
		}
		if(commandType == Command.C_PUSH || commandType == Command.C_POP || commandType == Command.C_FUNCTION || commandType == Command.C_CALL) {
			argument2 = Integer.parseInt(m.group(4));
		}
		return commandType;
	}
	
	String arg1(){
		return argument1;
	}
	
	int arg2(){
		return argument2;
	}
	

}
