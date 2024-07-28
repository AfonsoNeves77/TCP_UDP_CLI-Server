package org.example.anonymizedService;

import java.util.Arrays;

public class anonymize_Service {

    /**
     * Checks every word for any of the following characters at the end: [,][.][!][?]
     * It allows to consider any word to be anonymized right before a comma or at the end of a statement.
     * @param word Word to be checked
     * @return Word without special characters linked to it at the end
     */
    private String lastCharEvaluator(String word){
        while(lastCharChecker(word)){
            char[] letters = new char[word.length()-1];
            for(int i = 0; i < word.length()-1; i++){
                letters[i] = word.charAt(i);
            }
            word = String.valueOf(letters);
        }
        return word;
    }

    /**
     * Checks whether a given word is followed by one of scope's special characters.
     * Special characters considered: [,][.][!][?]
     * @param word Word to be checked
     * @return true if word is followed by a special character
     */
    private boolean lastCharChecker(String word){
        char lastChar =  word.charAt(word.length()-1);
        return lastChar == ',' || lastChar == '.' || lastChar == '!' || lastChar == '?';
    }

    /**
     * Converts the entire statement in the final result, i.e., anonymizes all the words to be considered.
     * It is not case-sensitive, which means it considers network, netWOrk and Network to be the same word.
     * @param phrase Statement to be considered
     * @param wordToBeAnonymized Word that is equal to the keyword chosen by the client
     * @return Final statement with all the anonymized occurrences of the keyword chosen
     */
    public String[] stringAnonymizer(String phrase, String wordToBeAnonymized){
        String[] anonymized = new String[2];
        int counter = 0;
        String[] phraseArray = phrase.split(" ");
        for(int i = 0; i < phraseArray.length; i++){
            if(lastCharEvaluator(phraseArray[i]).equalsIgnoreCase(wordToBeAnonymized)){
                phraseArray[i] = wordAnonymizer(phraseArray[i]);
                counter++;
            }
        }
        anonymized[0] = String.join(" ",phraseArray);
        anonymized[1] = Integer.toString(counter);

        return anonymized;
    }

    /**
     * Receives a String and converts it to an Array of chars with the value "X"
     * (1.) Transforms the String word into an Array of Chars.
     * (2.) Verifies if the last char is a special character. If it is, it only converts the non special chars to "X"
     * returning the converted word + special character (E.g. the String "Cat." would be returned as "XXX."
     * (3.) If the last char is not a special character. It converts the whole word into Xs. Returning it.
     * @param word String to anonymized
     * @return
     */
    private String wordAnonymizer(String word){
        // 1.
        char[] letters = new char[word.length()];
        for(int i = 0; i < word.length(); i++){
            letters[i] = word.charAt(i);
        }
        // 2.
        if(lastCharChecker(word)){
            for(int i = 0; i < word.length()-1; i++){
                letters[i] = 'X';
            }
            return String.valueOf(letters);
        }
        // 3.
        Arrays.fill(letters, 'X');
        return String.valueOf(letters);
    }
}
