package assembler;

import java.io.BufferedWriter;
import java.io.File;
import java.util.HashMap;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class HackAssembler {

	public static void main(String[] args) {
		try {
			//Initialization
			HashMap<String, Integer> label = new HashMap<>();;
			for(int i=0; i<16; i++) {
				label.put("R" + i,i);
			}
			label.put("SCREEN",16384);
			label.put("KBD",24576);
			label.put("SP",0);
			label.put("LCL",1);
			label.put("ARG",2);
			label.put("THIS",3);
			label.put("THAT",4);
			
			//First Pass
			String path = args[0];
			File file = new File(path);
			Scanner readerForLabels = new Scanner(file);
			int lineNumber = 0;
			int RAMAddress = 16;
			while (readerForLabels.hasNextLine()) {
				String currentLine = readerForLabels.nextLine();
				Pattern p = Pattern.compile("([^/]*)//(.*)");
				Matcher m = p.matcher(currentLine);
				if(m.find()) {
					currentLine = m.group(1);
				}
				if(currentLine != null && currentLine.length() != 0) {
					//finds and stores labels
					if(currentLine.charAt(0) == '(') {
						int end = currentLine.indexOf(')');
						label.put(currentLine.substring(1, end),lineNumber);
					}
					else { //not counting label lines
						lineNumber++;
					}
				}
			}
			readerForLabels.close();
			
			//Second Pass
			Scanner myReader = new Scanner(file);
			File fout = new File(file.getAbsolutePath().substring(0, file.getAbsolutePath().length()-3) + "hack");
			FileOutputStream fos = new FileOutputStream(fout);
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fos));

			while (myReader.hasNextLine()) {
				String currentLine = myReader.nextLine();
				currentLine = currentLine.replaceAll(" ", "");
				Pattern p = Pattern.compile("([^/]*)//(.*)");
				Matcher m = p.matcher(currentLine);
				if(m.find()) {
					currentLine = m.group(1);
				}
				if(currentLine == null || currentLine.length() == 0 || currentLine.charAt(0) == '(') {  //ignoring white space, comments and labels, using the above regex
					continue;
				}
				else {
					String lineBinary;
					short num;
					if(currentLine.charAt(0) == '@') { // if this is an A instruction
						if(label.containsKey(currentLine.substring(1))) {
							int value = label.get(currentLine.substring(1));
							num = (short)value;
						}
						else if(Character.isDigit(currentLine.charAt(1))) { 
							num = (short)Integer.parseInt(currentLine.substring(1));
						}
						else {
							label.put(currentLine.substring(1),RAMAddress);
							num = (short)RAMAddress;
							RAMAddress++;
						}
						lineBinary = "0" + String.format("%15s", Integer.toBinaryString(num & 0xFFFF)).replace(' ', '0');
					}
					else {  // if this is a C instruction
						p = Pattern.compile("(([^=]*)=)?([^;]*)(;(.*))?");
						m = p.matcher(currentLine);
						m.find();
						String dest = m.group(2);
						String comp = m.group(3);
						String jump = m.group(5);

						short destBinary;
						if(dest == null) {
							destBinary = 0;
						}
						else {
							switch (dest) {
							case "M":
								destBinary = 1;
								break;
							case "D":
								destBinary = 2;
								break;
							case "MD":
								destBinary = 3;
								break;
							case "A":
								destBinary = 4;
								break;
							case "AM":
								destBinary = 5;
								break;
							case "AD":
								destBinary = 6;
								break;
							default: //case "AMD":
								destBinary = 7;
								break;
							}
						}

						short jumpBinary;
						if(jump == null) {
							jumpBinary = 0;
						}
						else {
							switch (jump) {
							case "JGT":
								jumpBinary = 1;
								break;
							case "JEQ":
								jumpBinary = 2;
								break;
							case "JGE":
								jumpBinary = 3;
								break;
							case "JLT":
								jumpBinary = 4;
								break;
							case "JNE":
								jumpBinary = 5;
								break;
							case "JLE":
								jumpBinary = 6;
								break;
							default: //case "JMP":
								jumpBinary = 7;
								break;
							}
						}

						short compBinary;
						switch (comp) {
						case "0":
							compBinary = 42;
							break;
						case "1":
							compBinary = 63;
							break;
						case "-1":
							compBinary = 58;
							break;
						case "D":
							compBinary = 12;
							break;
						case "A":
							compBinary = 48;
							break;
						case "!D":
							compBinary = 13;
							break;
						case "!A":
							compBinary = 49;
							break;
						case "-D":
							compBinary = 15;
							break;
						case "-A":
							compBinary = 51;
							break;
						case "D+1":
							compBinary = 31;
							break;
						case "A+1":
							compBinary = 55;
							break;
						case "D-1":
							compBinary = 14;
							break;
						case "A-1":
							compBinary = 50;
							break;
						case "D+A":
							compBinary = 2;
							break;
						case "D-A":
							compBinary = 19;
							break;
						case "A-D":
							compBinary = 7;
							break;
						case "D&A":
							compBinary = 0;
							break;
						case "D|A":
							compBinary = 21;
							break;
						case "M":
							compBinary = 48 + 64; // + the 'a' bit
							break;
						case "!M":
							compBinary = 49 + 64;
							break;
						case "-M":
							compBinary = 51 + 64;
							break;
						case "M+1":
							compBinary = 55 + 64;
							break;
						case "M-1":
							compBinary = 50 + 64;
							break;
						case "D+M":
							compBinary = 2 + 64;
							break;
						case "D-M":
							compBinary = 19 + 64;
							break;
						case "M-D":
							compBinary = 7 + 64;
							break;
						case "D&M":
							compBinary = 64;
							break;
						default: //case "D|M":
							compBinary = 21 + 64;
							break;
						}

						short cInst = 7;
						lineBinary = Integer.toBinaryString(jumpBinary | (destBinary << 3) | (compBinary << 6) | (cInst << 13));
					}
					//Writing to file
					writer.write(lineBinary);
					writer.newLine();
				}
			}
			writer.close();
			myReader.close();
		} catch (FileNotFoundException e) {
			System.out.println("An error occurred.");
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
