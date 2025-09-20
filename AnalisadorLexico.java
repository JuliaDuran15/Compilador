import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

enum TokenSimbolo {
    sprograma, sinicio, sfim, sprocedimento, sfuncao,
    sse, sentao, ssenao, senquanto, sfaca,
    satribuicao, sescreva, sleia, svar, sinteiro, sbooleano,
    sidentificador, snumero,
    sponto, sponto_virgula, svirgula,
    sabre_parenteses, sfecha_parenteses,
    smaior, smaior_ig, sigual, smenor, smenor_ig, sdiferente,
    smais, smenos, smultiplicacao,
    sdiv, se, sou, snao,
    sdois_pontos,
    sverdadeiro, sfalso,
    serro,
    sfim_arquivo
}

class Token {
    TokenSimbolo simbolo;
    String lexema;
    int linha; // linha do token

    Token(TokenSimbolo simbolo, String lexema, int linha) {
        this.simbolo = simbolo;
        this.lexema = lexema;
        this.linha = linha;
    }

    public TokenSimbolo getSimbolo() {
        return simbolo;
    }
    
    public String getLexema() {
        return lexema;
    }
    
    public int getLinha() {
        return linha;
    }

    @Override
    public String toString() {
        return "Token: " + simbolo + " | Lexema: " + lexema + " | Linha: " + linha;
    }
}
public class AnalisadorLexico {
    private FileReader arquivo;
    private int caractere;
    private int linhaAtual = 1;
    private boolean erroComentarioNaoFechado = false;
    private int linhaErroComentario = -1;

    public AnalisadorLexico(String nomeArquivo) throws IOException {
        arquivo = new FileReader(nomeArquivo);
        lerCaractere();
    }

    // CORREÇÃO: leia primeiro, depois atualize a linha se for '\n'
    private void lerCaractere() throws IOException {
        caractere = arquivo.read();
        if (caractere == '\n') {
            linhaAtual++;
        }
    }

    // CORREÇÃO: loop para pular espaços e comentários repetidamente
    private void pulaEspacoComentario() throws IOException {
        while (caractere != -1) {
            // pula espaços em branco
            while (caractere != -1 && Character.isWhitespace(caractere)) {
                lerCaractere();
            }

            // se for comentário { ... }, consome até '}' e repete (pode haver espaços/comentários consecutivos)
            if (caractere == '{') {
                int linhaInicioComentario = linhaAtual;
                lerCaractere(); // consome '{'
                while (caractere != -1 && caractere != '}') {
                    lerCaractere();
                }
                if (caractere == -1) {
                    erroComentarioNaoFechado = true;
                    linhaErroComentario = linhaInicioComentario;
                    return; // comentário não fechado -> retorna erro
                } else {
                    lerCaractere(); // consome '}'
                    // continua o loop: pode ter whitespace ou outro comentário em seguida
                }
            } else {
                break; // nem espaço nem comentário: pronto para tokenizar
            }
        }
    }

    private Token trataDigito() throws IOException {
        StringBuilder lexema = new StringBuilder();
        int linhaToken = linhaAtual;
        while (caractere != -1 && Character.isDigit(caractere)) {
            lexema.append((char) caractere);
            lerCaractere();
        }
        return new Token(TokenSimbolo.snumero, lexema.toString(), linhaToken);
    }

    private Token trataIdentificadorPalavraReservada() throws IOException {
        StringBuilder lexema = new StringBuilder();
        int linhaToken = linhaAtual;
        while (caractere != -1 && (Character.isLetterOrDigit(caractere) || caractere == '_')) {
            lexema.append((char) caractere);
            lerCaractere();
        }
        String palavra = lexema.toString();
        switch (palavra) {
            case "programa": return new Token(TokenSimbolo.sprograma, palavra, linhaToken);
            case "inicio": return new Token(TokenSimbolo.sinicio, palavra, linhaToken);
            case "fim": return new Token(TokenSimbolo.sfim, palavra, linhaToken);
            case "procedimento": return new Token(TokenSimbolo.sprocedimento, palavra, linhaToken);
            case "funcao": return new Token(TokenSimbolo.sfuncao, palavra, linhaToken);
            case "se": return new Token(TokenSimbolo.sse, palavra, linhaToken);
            case "entao": return new Token(TokenSimbolo.sentao, palavra, linhaToken);
            case "senao": return new Token(TokenSimbolo.ssenao, palavra, linhaToken);
            case "enquanto": return new Token(TokenSimbolo.senquanto, palavra, linhaToken);
            case "faca": return new Token(TokenSimbolo.sfaca, palavra, linhaToken);
            case "escreva": return new Token(TokenSimbolo.sescreva, palavra, linhaToken);
            case "leia": return new Token(TokenSimbolo.sleia, palavra, linhaToken);
            case "var": return new Token(TokenSimbolo.svar, palavra, linhaToken);
            case "inteiro": return new Token(TokenSimbolo.sinteiro, palavra, linhaToken);
            case "booleano": return new Token(TokenSimbolo.sbooleano, palavra, linhaToken);
            case "verdadeiro": return new Token(TokenSimbolo.sverdadeiro, palavra, linhaToken);
            case "falso": return new Token(TokenSimbolo.sfalso, palavra, linhaToken);
            case "div": return new Token(TokenSimbolo.sdiv, palavra, linhaToken);
            case "e": return new Token(TokenSimbolo.se, palavra, linhaToken);
            case "ou": return new Token(TokenSimbolo.sou, palavra, linhaToken);
            case "nao": return new Token(TokenSimbolo.snao, palavra, linhaToken);
            default: return new Token(TokenSimbolo.sidentificador, palavra, linhaToken);
        }
    }

    private Token trataSimbolos() throws IOException {
        char c = (char) caractere;
        int linhaToken = linhaAtual;
        lerCaractere();
        switch (c) {
            case ':':
                if (caractere == '=') { lerCaractere(); return new Token(TokenSimbolo.satribuicao, ":=", linhaToken); }
                else return new Token(TokenSimbolo.sdois_pontos, ":", linhaToken);
            case '>':
                if (caractere == '=') { lerCaractere(); return new Token(TokenSimbolo.smaior_ig, ">=", linhaToken); }
                else return new Token(TokenSimbolo.smaior, ">", linhaToken); 
            case '<':
                if (caractere == '=') { lerCaractere(); return new Token(TokenSimbolo.smenor_ig, "<=", linhaToken); }
                else return new Token(TokenSimbolo.smenor, "<", linhaToken); 
            case '!':
                if (caractere == '=') { lerCaractere(); return new Token(TokenSimbolo.sdiferente, "!=", linhaToken); }
                else { return new Token(TokenSimbolo.serro, "!", linhaToken); }
            case '=': return new Token(TokenSimbolo.sigual, "=", linhaToken);
            case '+': return new Token(TokenSimbolo.smais, "+", linhaToken);
            case '-': return new Token(TokenSimbolo.smenos, "-", linhaToken);
            case '*': return new Token(TokenSimbolo.smultiplicacao, "*", linhaToken);
            case '.': return new Token(TokenSimbolo.sponto, ".", linhaToken);
            case ';': return new Token(TokenSimbolo.sponto_virgula, ";", linhaToken);
            case ',': return new Token(TokenSimbolo.svirgula, ",", linhaToken);
            case '(': return new Token(TokenSimbolo.sabre_parenteses, "(", linhaToken);
            case ')': return new Token(TokenSimbolo.sfecha_parenteses, ")", linhaToken);
            default: return new Token(TokenSimbolo.serro, String.valueOf(c), linhaToken);
        }
    }

    public Token pegaToken() throws IOException {
        pulaEspacoComentario();

        if (erroComentarioNaoFechado) {
            Token erro = new Token(TokenSimbolo.serro, "Comentario nao fechado", linhaErroComentario);
            erroComentarioNaoFechado = false;
            return erro;
        }

        if (caractere == -1) {
            return new Token(TokenSimbolo.sfim_arquivo, "EOF", linhaAtual);
        }
        
        char c = (char) caractere;
        if (Character.isDigit(c)) {
            return trataDigito();
        } else if (Character.isLetter(c)) {
            return trataIdentificadorPalavraReservada();
        } else {
            return trataSimbolos();
        }
    }
    
    public void fechar() throws IOException {
        if (arquivo != null) {
            arquivo.close();
        }
    }
}