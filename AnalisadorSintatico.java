import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AnalisadorSintatico {
    private Token tokenAtual;
    private TabelaSimbolos tabela;
    private AnalisadorLexico lexico;

    public AnalisadorSintatico(AnalisadorLexico lexico, TabelaSimbolos tabela) throws IOException {
        this.lexico = lexico;
        this.tabela = tabela;
        proximoToken();
    }

    private void proximoToken() throws IOException {
        tokenAtual = lexico.pegaToken();
    }

    private void erro(String msg) {
        if (tokenAtual != null)
            throw new RuntimeException("Erro sintatico na linha " + tokenAtual.getLinha() + ": " + msg);
        else
            throw new RuntimeException("Erro sintatico: " + msg + " (Fim inesperado do arquivo)");
    }

    // <programa> ::= programa <identificador> ; <bloco> .
    public void analisaPrograma() throws IOException {
        if (tokenAtual.getSimbolo() != TokenSimbolo.sprograma)
            erro("Palavra-chave 'programa' esperada");

        proximoToken();

        if (tokenAtual.getSimbolo() != TokenSimbolo.sidentificador)
            erro("Identificador esperado apos 'programa'");

        tabela.inserir(tokenAtual.getLexema(), tabela.getNivelAtual(), "programa");
        proximoToken();

        if (tokenAtual.getSimbolo() != TokenSimbolo.sponto_virgula)
            erro("Ponto e virgula esperado apos identificador do programa");

        proximoToken();
        analisaBloco();

        // verificação do ponto final
        if (tokenAtual.getSimbolo() == TokenSimbolo.sponto_virgula)
            erro("Ponto e virgula nao permitido apos 'fim' do programa principal");

        if (tokenAtual.getSimbolo() == TokenSimbolo.sponto) {
            proximoToken();
            System.out.println("Programa valido!");
        } else {
            erro("Ponto final esperado apos 'fim' do programa principal");
        }
    }

    // <bloco> ::= [<etapa de declaracao de variaveis>] [<etapa de declaracao de sub-rotinas>] <comandos>
    private void analisaBloco() throws IOException {
        tabela.entrarEscopo();
        analisaEtVariaveis();
        analisaSubrotinas();
        analisaComandos();
        tabela.sairEscopo();
    }

    // <etapa de declaracao de variaveis> ::= var <declaracao de variaveis> ; {<declaracao de variaveis>;}
    private void analisaEtVariaveis() throws IOException {
        if (tokenAtual.getSimbolo() == TokenSimbolo.svar) {
            proximoToken();
            analisaDeclaracaoVariaveis();

            while (tokenAtual.getSimbolo() == TokenSimbolo.sponto_virgula) {
                proximoToken();
                if (tokenAtual.getSimbolo() == TokenSimbolo.sidentificador)
                    analisaDeclaracaoVariaveis();
                else
                    break;
            }
        }
    }

    private void analisaDeclaracaoVariaveis() throws IOException {
        List<String> ids = new ArrayList<>();

        if (tokenAtual.getSimbolo() != TokenSimbolo.sidentificador)
            erro("Identificador esperado na declaracao de variaveis");

        ids.add(tokenAtual.getLexema());
        proximoToken();

        while (tokenAtual.getSimbolo() == TokenSimbolo.svirgula) {
            proximoToken();
            if (tokenAtual.getSimbolo() != TokenSimbolo.sidentificador)
                erro("Identificador esperado apos virgula");
            ids.add(tokenAtual.getLexema());
            proximoToken();
        }

        if (tokenAtual.getSimbolo() != TokenSimbolo.sdois_pontos)
            erro("Dois pontos esperado apos lista de identificadores");

        proximoToken();
        String tipo = analisaTipo();

        for (String id : ids) {
            if (!tabela.inserir(id, tabela.getNivelAtual(), tipo))
                erro("Identificador '" + id + "' ja declarado neste escopo");
        }
    }

    private String analisaTipo() throws IOException {
        if (tokenAtual.getSimbolo() == TokenSimbolo.sinteiro ||
            tokenAtual.getSimbolo() == TokenSimbolo.sbooleano) {
            String tipo = tokenAtual.getLexema();
            proximoToken();
            return tipo;
        } else {
            erro("Tipo esperado (inteiro ou booleano)");
            return null;
        }
    }

    // <etapa de declaracao de sub-rotinas> ::= (<declaracao de procedimento>; | <declaracao de funcao>;){...}
    private void analisaSubrotinas() throws IOException {
        while (tokenAtual.getSimbolo() == TokenSimbolo.sprocedimento || tokenAtual.getSimbolo() == TokenSimbolo.sfuncao) {
            if (tokenAtual.getSimbolo() == TokenSimbolo.sprocedimento)
                analisaDeclaracaoProcedimento();
            else
                analisaDeclaracaoFuncao();

            if (tokenAtual.getSimbolo() != TokenSimbolo.sponto_virgula)
                erro("Ponto e virgula esperado apos declaracao de sub-rotina");
            proximoToken();
        }
    }

    private void analisaDeclaracaoProcedimento() throws IOException {
        proximoToken();
        if (tokenAtual.getSimbolo() != TokenSimbolo.sidentificador)
            erro("Identificador esperado apos 'procedimento'");

        if (!tabela.inserir(tokenAtual.getLexema(), tabela.getNivelAtual(), "procedimento"))
            erro("Procedimento '" + tokenAtual.getLexema() + "' ja declarado");

        proximoToken();

        if (tokenAtual.getSimbolo() != TokenSimbolo.sponto_virgula)
            erro("Ponto e virgula esperado apos cabecalho de procedimento");

        proximoToken();
        analisaBloco();
    }

    private void analisaDeclaracaoFuncao() throws IOException {
        proximoToken();
        if (tokenAtual.getSimbolo() != TokenSimbolo.sidentificador)
            erro("Identificador esperado apos 'funcao'");

        String nome = tokenAtual.getLexema();
        proximoToken();

        if (tokenAtual.getSimbolo() != TokenSimbolo.sdois_pontos)
            erro("':' esperado apos nome da funcao");

        proximoToken();
        String tipo = analisaTipo();

        if (!tabela.inserir(nome, tabela.getNivelAtual(), tipo))
            erro("Funcao '" + nome + "' ja declarada");

        if (tokenAtual.getSimbolo() != TokenSimbolo.sponto_virgula)
            erro("Ponto e virgula esperado apos tipo da funcao");

        proximoToken();
        analisaBloco();
    }

    // <comandos> ::= inicio <comando> {; <comando>} [;] fim
    private void analisaComandos() throws IOException {
        if (tokenAtual.getSimbolo() != TokenSimbolo.sinicio)
            erro("'inicio' esperado");

        proximoToken();
        analisaComando();

        while (tokenAtual.getSimbolo() == TokenSimbolo.sponto_virgula) {
            proximoToken();
            if (tokenAtual.getSimbolo() == TokenSimbolo.sfim)
                break;
            analisaComando();
        }

        if (tokenAtual.getSimbolo() != TokenSimbolo.sfim)
            erro("'fim' esperado");

        proximoToken(); // consome fim
    }

    private void analisaComando() throws IOException {
        switch (tokenAtual.getSimbolo()) {
            case sidentificador:
                analisaAtribOuChamada();
                break;
            case sse:
                analisaSe();
                break;
            case senquanto:
                analisaEnquanto();
                break;
            case sleia:
                analisaLeia();
                break;
            case sescreva:
                analisaEscreva();
                break;
            case sinicio:
                analisaComandos();
                break;
            default:
                erro("Comando invalido");
        }
    }

    // <atribuição_chprocedimento> ::= <identificador> := <expressao> | <identificador>
    private void analisaAtribOuChamada() throws IOException {
        Simbolo s = tabela.buscar(tokenAtual.getLexema());
        if (s == null)
            erro("Identificador '" + tokenAtual.getLexema() + "' nao declarado");

        String nome = tokenAtual.getLexema();
        proximoToken();

        if (tokenAtual.getSimbolo() == TokenSimbolo.satribuicao) {
            if (s.getTipo().equals("procedimento") || s.getTipo().equals("programa"))
                erro("Nao e possivel atribuir a '" + nome + "' (tipo " + s.getTipo() + ")");
            proximoToken();
            analisaExpressaoComTipo();
        } else {
            // chamada simples de procedimento (sem parenteses)
            if (!s.getTipo().equals("procedimento"))
                erro("Chamada invalida: '" + nome + "' nao e um procedimento");
        }
    }

    private void analisaSe() throws IOException {
        proximoToken();
        analisaExpressaoComTipo();
        if (tokenAtual.getSimbolo() != TokenSimbolo.sentao)
            erro("'entao' esperado");
        proximoToken();
        analisaComando();
        if (tokenAtual.getSimbolo() == TokenSimbolo.ssenao) {
            proximoToken();
            analisaComando();
        }
    }

    private void analisaEnquanto() throws IOException {
        proximoToken();
        analisaExpressaoComTipo();
        if (tokenAtual.getSimbolo() != TokenSimbolo.sfaca)
            erro("'faca' esperado apos expressao do 'enquanto'");
        proximoToken();
        analisaComando();
    }

    private void analisaLeia() throws IOException {
        proximoToken();
        if (tokenAtual.getSimbolo() != TokenSimbolo.sabre_parenteses)
            erro("'(' esperado apos 'leia'");
        proximoToken();
        if (tokenAtual.getSimbolo() != TokenSimbolo.sidentificador)
            erro("Identificador esperado em 'leia'");
        proximoToken();
        if (tokenAtual.getSimbolo() != TokenSimbolo.sfecha_parenteses)
            erro("')' esperado apos identificador em 'leia'");
        proximoToken();
    }

    private void analisaEscreva() throws IOException {
        proximoToken();
        if (tokenAtual.getSimbolo() != TokenSimbolo.sabre_parenteses)
            erro("'(' esperado apos 'escreva'");
        proximoToken();
        if (tokenAtual.getSimbolo() != TokenSimbolo.sidentificador)
            erro("Identificador esperado em 'escreva'");
        proximoToken();
        if (tokenAtual.getSimbolo() != TokenSimbolo.sfecha_parenteses)
            erro("')' esperado apos identificador em 'escreva'");
        proximoToken();
    }

    // EXPRESSOES
    private String analisaExpressaoComTipo() throws IOException {
        String tipo1 = analisaExpressaoSimplesComTipo();
        if (tokenAtual.getSimbolo() == TokenSimbolo.sigual || tokenAtual.getSimbolo() == TokenSimbolo.sdiferente ||
            tokenAtual.getSimbolo() == TokenSimbolo.smaior || tokenAtual.getSimbolo() == TokenSimbolo.smenor ||
            tokenAtual.getSimbolo() == TokenSimbolo.smaior_ig || tokenAtual.getSimbolo() == TokenSimbolo.smenor_ig) {
            proximoToken();
            analisaExpressaoSimplesComTipo();
            return "booleano";
        }
        return tipo1;
    }

    private String analisaExpressaoSimplesComTipo() throws IOException {
        if (tokenAtual.getSimbolo() == TokenSimbolo.smais || tokenAtual.getSimbolo() == TokenSimbolo.smenos)
            proximoToken();
        analisaTermoComTipo();
        while (tokenAtual.getSimbolo() == TokenSimbolo.smais ||
               tokenAtual.getSimbolo() == TokenSimbolo.smenos ||
               tokenAtual.getSimbolo() == TokenSimbolo.sou) {
            proximoToken();
            analisaTermoComTipo();
        }
        return "inteiro"; // simplificado
    }

    private String analisaTermoComTipo() throws IOException {
        analisaFatorComTipo();
        while (tokenAtual.getSimbolo() == TokenSimbolo.smultiplicacao ||
               tokenAtual.getSimbolo() == TokenSimbolo.sdiv ||
               tokenAtual.getSimbolo() == TokenSimbolo.se) {
            proximoToken();
            analisaFatorComTipo();
        }
        return "inteiro";
    }

    private String analisaFatorComTipo() throws IOException {
        switch (tokenAtual.getSimbolo()) {
            case sidentificador:
                proximoToken();
                return "inteiro";
            case snumero:
                proximoToken();
                return "inteiro";
            case sverdadeiro:
            case sfalso:
                proximoToken();
                return "booleano";
            case snao:
                proximoToken();
                analisaFatorComTipo();
                return "booleano";
            case sabre_parenteses:
                proximoToken();
                analisaExpressaoComTipo();
                if (tokenAtual.getSimbolo() != TokenSimbolo.sfecha_parenteses)
                    erro("')' esperado");
                proximoToken();
                return "inteiro";
            default:
                erro("Fator invalido");
                return null;
        }
    }


    private void analisaExpressao() throws IOException {
        analisaExpressaoSimples();
        if (tokenAtual.getSimbolo() == TokenSimbolo.smaior || tokenAtual.getSimbolo() == TokenSimbolo.smaior_ig ||
            tokenAtual.getSimbolo() == TokenSimbolo.sigual || tokenAtual.getSimbolo() == TokenSimbolo.smenor ||
            tokenAtual.getSimbolo() == TokenSimbolo.smenor_ig || tokenAtual.getSimbolo() == TokenSimbolo.sdiferente) {
            proximoToken();
            analisaExpressaoSimples();
        }
    }

    private void analisaExpressaoSimples() throws IOException {
        if (tokenAtual.getSimbolo() == TokenSimbolo.smais || tokenAtual.getSimbolo() == TokenSimbolo.smenos) {
            proximoToken();
        }
        analisaTermo();
        while (tokenAtual.getSimbolo() == TokenSimbolo.smais || tokenAtual.getSimbolo() == TokenSimbolo.smenos ||
               tokenAtual.getSimbolo() == TokenSimbolo.sou) {
            proximoToken();
            analisaTermo();
        }
    }

    private void analisaTermo() throws IOException {
        analisaFator();
        while (tokenAtual.getSimbolo() == TokenSimbolo.smultiplicacao || tokenAtual.getSimbolo() == TokenSimbolo.sdiv ||
               tokenAtual.getSimbolo() == TokenSimbolo.se) {
            proximoToken();
            analisaFator();
        }
    }

    private void analisaFator() throws IOException {
        if (tokenAtual.getSimbolo() == TokenSimbolo.sidentificador) {
            if (tabela.buscar(tokenAtual.getLexema()) != null) {
                proximoToken();
            } else {
                erro("Identificador '" + tokenAtual.getLexema() + "' nao declarado");
            }
        } else if (tokenAtual.getSimbolo() == TokenSimbolo.snumero) {
            proximoToken();
        } else if (tokenAtual.getSimbolo() == TokenSimbolo.snao) {
            proximoToken();
            analisaFator();
        } else if (tokenAtual.getSimbolo() == TokenSimbolo.sabre_parenteses) {
            proximoToken();
            analisaExpressao();
            if (tokenAtual.getSimbolo() == TokenSimbolo.sfecha_parenteses) {
                proximoToken();
            } else {
                erro("Parentese fechando esperado");
            }
        } else if (tokenAtual.getSimbolo() == TokenSimbolo.sverdadeiro || tokenAtual.getSimbolo() == TokenSimbolo.sfalso) {
            proximoToken();
        } else {
            erro("Fator invalido");
        }
    }

}