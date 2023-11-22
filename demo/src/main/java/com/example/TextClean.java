package com.example;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import lemport.lemma.Lemmatizer;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;

public class TextClean {

    public TextClean(String path) throws IOException {
        // Ordem esperada
        // String text = LoadText(path);
        // text = Regex(text);
        // text = StopWords(text);
        // Lematizacao(Tokenizacao(text), POSTagger(text));
    }

    // Le o nosso arquivo txt
    static TextDetails LoadText(String path) throws IOException {
        Path filePath = Path.of(path);

        StringBuilder sb = new StringBuilder();
        String lastLine = "";

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(filePath.toFile()), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line.toLowerCase()).append("\n");
                lastLine = line;
            }
        }

        String text = sb.toString();
        // Remover URL's do texto
        String pattern = ("((http|https)://)(www.)?[a-zA-Z0-9@:%._\\+~#?&//=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%._\\+~#?&//=]*)");
        text = Pattern.compile(pattern, Pattern.MULTILINE).matcher(text).replaceAll("");

        // Extract authors from the last line
        String[] autores = lastLine.split(",");

        TextDetails textDetails = new TextDetails(text, autores);

        return textDetails;
    }

    // Limpeza Regex
    static String Regex(String text) {

        String pattern;

        // Tira pontuações menos "." para ser possível fazer a separação por frase
        pattern = "([\\p{P}\\p{S}&&[^-]])";
        text = Pattern.compile(pattern, Pattern.MULTILINE).matcher(text).replaceAll("");

        // Tira digitos numericos
        pattern = "\\d";
        text = Pattern.compile(pattern, Pattern.MULTILINE).matcher(text).replaceAll("");

        return text;
    }

    // Tokenizacao
    static String[] Tokenizacao(String rawText) {
        // Usa um modelo pre treinado para fazer a tokenização com maxima entropia
        try (InputStream modelInputStream = new FileInputStream(new File("demo\\models\\pt-token.bin"))) {
            TokenizerModel tokenizerModel = new TokenizerModel(modelInputStream);
            TokenizerME tokenizer = new TokenizerME(tokenizerModel);

            String[] tokens = tokenizer.tokenize(rawText);
            return tokens;

        } catch (FileNotFoundException e) {
            // Handle exception
            return null;
        } catch (IOException e) {
            // Handle exception
            return null;
        }
    }

    // Limpeza
    static String StopWords(String text) throws IOException {
        // Le o nosso arquivo 'stopwords.txt'
        Path filePath = Path.of("demo\\models\\stopwords.txt");
        List<String> tokenizedText = Arrays.asList(Tokenizacao(text));

        List<String> sotpWords = Files.readAllLines(filePath);

        // Converte o texto para lowerCase
        tokenizedText = tokenizedText.stream().map(line -> line).collect(Collectors.toList());

        // Remove todas as palavras em stopWords do nosso arquivo
        tokenizedText.removeAll(sotpWords);

        String result = tokenizedText.stream().map(n -> String.valueOf(n))
                .collect(Collectors.joining(" ", "", ""));

        return result;
    }

    // Tags das palavras
    static String[] POSTagger(String[] tokens) {
        // Utiliza um modelo pre treinado para fazer a Tag com maxima entropia
        try (InputStream modelInputStream = new FileInputStream(new File("demo\\models\\pt-pos-maxent.bin"));) {
            POSModel posModel = new POSModel(modelInputStream);
            POSTaggerME posTaggerME = new POSTaggerME(posModel);

            // Gera as tags com base nos tokens das palavras
            String tags[] = posTaggerME.tag(tokens);

            return tags;
        } catch (IOException e) {
            // Handle exceptions
            return null;
        }
    }

    // Lematiza o texto ja tokenizado utilizando as Tags
    static String[] Lematizacao(String[] tokenizedText, String[] tagedText) {
        String[] tokens = tokenizedText;
        String[] tags = tagedText;

        // Listas auxiliares para poder retirar os verbos e remontar o Array
        List<String> tokensL = new ArrayList<>(Arrays.asList(tokens));
        List<String> tagsL = new ArrayList<>(Arrays.asList(tags));

        final Lemmatizer lemmatizer;
        final String[] lemmas;
        try {

            // Retira os verbos
            for (int i = tagsL.size() - 1; i >= 0; i--) {

                if (tagsL.get(i).startsWith("v")) {
                    tokensL.remove(i);
                    tagsL.remove(i);
                }
            }

            // Remontagem dos Arrays sem os verbos
            tokens = tokensL.toArray(new String[0]);
            tags = tagsL.toArray(new String[0]);

            lemmatizer = new Lemmatizer();
            lemmas = lemmatizer.lemmatize(tokens, tags);

            // Retorna as palavras ascentuadas para uma forma sem elas
            for (int i = 0; i < lemmas.length; i++) {
                lemmas[i] = Normalizer.normalize(lemmas[i], Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "")
                        .toLowerCase();
            }

            return lemmas;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
