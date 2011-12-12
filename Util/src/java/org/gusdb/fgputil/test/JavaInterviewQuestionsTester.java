package org.gusdb.fgputil.test;

import java.io.File;
import java.io.IOException;

public class JavaInterviewQuestionsTester extends UnitTest {
	
	private static final String REVERSABLE_FILE_NAME = "org/gusdb/fgputil/test/reversableTextFile.txt";
	private static final String REVERSED_FILE_NAME_CORRECT = "org/gusdb/fgputil/test/reversedTextFile.txt";
	private static final String REVERSED_FILE_PATH = "/tmp/reversedTextFile.txt";
	
	public static void main(String[] args) {
		JavaInterviewQuestionsTester test = new JavaInterviewQuestionsTester();
		test.testPalindrome();
		test.testNearPalindrome();
		test.testReverseFile();
		test.testUniqueChars();
		test.printResults();
	}
	
	private void testPalindrome() {
		JavaInterviewQuestions q = new JavaInterviewQuestions();
		assertFalse(q.isPalindrome(null));
		assertTrue(q.isPalindrome(""));
		assertTrue(q.isPalindrome("aba"));
		assertTrue(q.isPalindrome("abba"));
		assertTrue(q.isPalindrome("quvicoekeocivuq"));
		assertFalse(q.isPalindrome("abca"));
		assertFalse(q.isPalindrome("uiopoui"));
		assertFalse(q.isPalindrome("yuijuhiuy"));
	}

	private void testNearPalindrome() {
		JavaInterviewQuestions q = new JavaInterviewQuestions();
		assertFalse(q.isNearPalindrome(null, 500));
		assertTrue(q.isNearPalindrome("", 0));
		assertTrue(q.isNearPalindrome("aba", 0));
		assertTrue(q.isNearPalindrome("abba", 5));
		assertTrue(q.isNearPalindrome("quvicoekeocivuq", -5));
		assertFalse(q.isNearPalindrome("abca", 0));
		assertTrue(q.isNearPalindrome("abca", 1));
		assertFalse(q.isNearPalindrome("abcd", 1));
		assertTrue(q.isNearPalindrome("abcd", 2));
		assertTrue(q.isNearPalindrome("abcda", 1));
		assertFalse(q.isNearPalindrome("uiopoui", 1));
		assertTrue(q.isNearPalindrome("uiopoui", 2));
		assertFalse(q.isNearPalindrome("yuijuhiuy",0));
		assertTrue(q.isNearPalindrome("yuijuhiuy",1));
		assertFalse(q.isNearPalindrome("abracadabra",2));
		assertTrue(q.isNearPalindrome("abracadabra",3));
		assertTrue(q.isNearPalindrome("abracadabra",20));
	}
	
	private void testReverseFile() {
		JavaInterviewQuestions q = new JavaInterviewQuestions();
		try {
			String inputFilePath = getResourceFilePath(REVERSABLE_FILE_NAME);
			String verificationFilePath = getResourceFilePath(REVERSED_FILE_NAME_CORRECT);
			
			// easy way
			q.reverseFile(inputFilePath, REVERSED_FILE_PATH);
			assertFilesEqual(REVERSED_FILE_PATH, verificationFilePath);
			new File(REVERSED_FILE_PATH).delete();
			
			// memory-safe way
			q.reverseFileMemSafe(inputFilePath, REVERSED_FILE_PATH);
			assertFilesEqual(REVERSED_FILE_PATH, verificationFilePath);
			new File(REVERSED_FILE_PATH).delete();
		}
		catch (IOException ioe) {
			throw new RuntimeException("I/O Error", ioe);
		}
	}
	
	private void testUniqueChars() {
		JavaInterviewQuestions q = new JavaInterviewQuestions();
		String[][] testSet = {
				{ "tttttthhhhthhhthhth", "ht" },
				{ "aniapoisydflkasndoipvine", "adefiklnopsvy" },
				{ "abddebcaecd", "abcde" },
				{ "fasd", "adfs" }
		};
		for (String[] testPair : testSet) {
			assertEqual(q.getUniqueChars(testPair[0]), testPair[1]);
			assertEqual(q.getUniqueCharsBetter(testPair[0]), testPair[1]);
		}
	}
}
