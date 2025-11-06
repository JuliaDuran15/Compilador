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
            throw new RuntimeException("Erro sintatico/semantico na linha " + tokenAtual.getLinha() + ": " + msg);
        else
            throw new RuntimeException("Erro sintatico/semantico: " + msg + " (Fim inesperado do arquivo)");
    }

    // <programa> ::= programa <identificador> ; <bloco> .
    public void analisaPrograma() throws IOException {
        if (tokenAtual.getSimbolo() != TokenSimbolo.sprograma)
            erro("Palavra-chave 'programa' esperada");

        proximoToken();

        if (tokenAtual.getSimbolo() != TokenSimbolo.sidentificador)
            erro("Identificador esperado apos 'programa'");

        String nomePrograma = tokenAtual.getLexema();
        if (!tabela.inserir(nomePrograma, tabela.getNivelAtual(), "programa"))
            erro("Nome do programa '" + nomePrograma + "' ja declarado em escopo visivel");

        proximoToken();

        if (tokenAtual.getSimbolo() != TokenSimbolo.sponto_virgula)
            erro("Ponto e virgula esperado apos identificador do programa");

        proximoToken();
        analisaBloco();

        if (tokenAtual.getSimbolo() == TokenSimbolo.sponto_virgula)
            erro("Ponto e virgula nao permitido apos 'fim' do programa principal");

        if (tokenAtual.getSimbolo() == TokenSimbolo.sponto) {
            proximoToken();
            System.out.println("Programa valido!");
        } else {
            erro("Ponto final esperado apos 'fim' do programa principal");
        }
    }

    private void analisaBloco() throws IOException {
        tabela.entrarEscopo();
        analisaEtVariaveis();
        analisaSubrotinas();
        analisaComandos();
        tabela.sairEscopo();
    }

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
            if (tabela.buscarNoNivelAtual(id) != null)
                erro("Identificador '" + id + "' ja declarado neste escopo");
            if (!tabela.inserir(id, tabela.getNivelAtual(), tipo))
                erro("Falha ao inserir identificador '" + id + "'");
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

        String nome = tokenAtual.getLexema();

        if (tabela.buscar(nome) != null)
            erro("Procedimento '" + nome + "' ja declarado (identificador visivel com mesmo nome)");
        if (!tabela.inserir(nome, tabela.getNivelAtual(), "procedimento"))
            erro("Nao foi possivel inserir procedimento '" + nome + "'");

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

        if (tabela.buscar(nome) != null)
            erro("Funcao '" + nome + "' ja declarada (identificador visivel com mesmo nome)");

        proximoToken();

        if (tokenAtual.getSimbolo() != TokenSimbolo.sdois_pontos)
            erro("':' esperado apos nome da funcao");

        proximoToken();
        String tipo = analisaTipo();

        if (!tabela.inserir(nome, tabela.getNivelAtual(), tipo))
            erro("Nao foi possivel inserir funcao '" + nome + "'");

        if (tokenAtual.getSimbolo() != TokenSimbolo.sponto_virgula)
            erro("Ponto e virgula esperado apos tipo da funcao");

        proximoToken();
        analisaBloco();
    }

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
        String lex = tokenAtual.getLexema();
        Simbolo s = tabela.buscar(lex);
        if (s == null)
            erro("Identificador '" + lex + "' nao declarado");

        String nome = lex;
        String tipoId = s.getTipo();

        proximoToken();

        // >>> ajustado: detectar ':' isolado (sdois_pontos) e dar mensagem clara
        if (tokenAtual.getSimbolo() == TokenSimbolo.sdois_pontos) {
            erro("Token ':' encontrado apos identificador. Para atribuicao use ':=' (dois caracteres).");
        }

        if (tokenAtual.getSimbolo() == TokenSimbolo.satribuicao) {
            if (tipoId.equals("procedimento") || tipoId.equals("programa"))
                erro("Nao e possivel atribuir a '" + nome + "' (tipo " + tipoId + ")");
            proximoToken();
            String tipoExpr = analisaExpressaoComTipo();
            if (!tipoId.equals(tipoExpr))
                erro("Incompatibilidade de tipos na atribuicao: '" + nome + "' e do tipo " + tipoId +
                     ", mas a expressao e do tipo " + tipoExpr);
        } else {
            // caso o token seguinte não seja ':=' => só faz sentido se for chamada de procedimento
            if (!tipoId.equals("procedimento"))
                erro("Chamada invalida: '" + nome + "' nao e um procedimento");
            // se for procedimento, a chamada simples é aceita (sem parâmetros)
        }
    }

    private void analisaSe() throws IOException {
        proximoToken();
        String tipoExpr = analisaExpressaoComTipo();
        if (!"booleano".equals(tipoExpr))
            erro("Expressao do 'se' deve ser booleana");
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
        String tipoExpr = analisaExpressaoComTipo();
        if (!"booleano".equals(tipoExpr))
            erro("Expressao do 'enquanto' deve ser booleana");
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

        Simbolo s = tabela.buscar(tokenAtual.getLexema());
        if (s == null)
            erro("Identificador '" + tokenAtual.getLexema() + "' nao declarado");
        if (!"inteiro".equals(s.getTipo()))
            erro("Comando 'leia' so pode ser usado com variaveis inteiras");

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

        Simbolo s = tabela.buscar(tokenAtual.getLexema());
        if (s == null)
            erro("Identificador '" + tokenAtual.getLexema() + "' nao declarado");
        if (!"inteiro".equals(s.getTipo()))
            erro("Comando 'escreva' so pode ser usado com variaveis inteiras");

        proximoToken();
        if (tokenAtual.getSimbolo() != TokenSimbolo.sfecha_parenteses)
            erro("')' esperado apos identificador em 'escreva'");
        proximoToken();
    }

    // EXPRESSOES (tipadas)
    private String analisaExpressaoComTipo() throws IOException {
        String tipo1 = analisaExpressaoSimplesComTipo();
        if (tokenAtual.getSimbolo() == TokenSimbolo.sigual || tokenAtual.getSimbolo() == TokenSimbolo.sdiferente ||
            tokenAtual.getSimbolo() == TokenSimbolo.smaior || tokenAtual.getSimbolo() == TokenSimbolo.smenor ||
            tokenAtual.getSimbolo() == TokenSimbolo.smaior_ig || tokenAtual.getSimbolo() == TokenSimbolo.smenor_ig) {
            TokenSimbolo op = tokenAtual.getSimbolo();
            proximoToken();
            String tipo2 = analisaExpressaoSimplesComTipo();
            if (!tipo1.equals(tipo2))
                erro("Incompatibilidade de tipos em comparacao: " + tipo1 + " " + op + " " + tipo2);
            return "booleano";
        }
        return tipo1;
    }

    private String analisaExpressaoSimplesComTipo() throws IOException {
        if (tokenAtual.getSimbolo() == TokenSimbolo.smais || tokenAtual.getSimbolo() == TokenSimbolo.smenos) {
            TokenSimbolo sinal = tokenAtual.getSimbolo();
            proximoToken();
            String tipoDepois = analisaTermoComTipo();
            if (!"inteiro".equals(tipoDepois))
                erro("Operador unario '" + (sinal==TokenSimbolo.smais?"+":"-") + "' so pode ser aplicado a inteiros");
            while (tokenAtual.getSimbolo() == TokenSimbolo.smais ||
                   tokenAtual.getSimbolo() == TokenSimbolo.smenos ||
                   tokenAtual.getSimbolo() == TokenSimbolo.sou) {
                TokenSimbolo op = tokenAtual.getSimbolo();
                proximoToken();
                String tipo2 = analisaTermoComTipo();
                if (op == TokenSimbolo.sou) {
                    if (!"booleano".equals(tipoDepois) || !"booleano".equals(tipo2))
                        erro("Operador 'ou' exige operandos booleanos");
                    tipoDepois = "booleano";
                } else {
                    if (!"inteiro".equals(tipoDepois) || !"inteiro".equals(tipo2))
                        erro("Operacao aritmetica exige operandos inteiros");
                    tipoDepois = "inteiro";
                }
            }
            return tipoDepois;
        } else {
            String tipo = analisaTermoComTipo();
            while (tokenAtual.getSimbolo() == TokenSimbolo.smais ||
                   tokenAtual.getSimbolo() == TokenSimbolo.smenos ||
                   tokenAtual.getSimbolo() == TokenSimbolo.sou) {
                TokenSimbolo op = tokenAtual.getSimbolo();
                proximoToken();
                String tipo2 = analisaTermoComTipo();
                if (op == TokenSimbolo.sou) {
                    if (!"booleano".equals(tipo) || !"booleano".equals(tipo2))
                        erro("Operador 'ou' exige operandos booleanos");
                    tipo = "booleano";
                } else {
                    if (!"inteiro".equals(tipo) || !"inteiro".equals(tipo2))
                        erro("Operacao aritmetica exige operandos inteiros");
                    tipo = "inteiro";
                }
            }
            return tipo;
        }
    }

    private String analisaTermoComTipo() throws IOException {
        String tipo = analisaFatorComTipo();
        while (tokenAtual.getSimbolo() == TokenSimbolo.smultiplicacao ||
               tokenAtual.getSimbolo() == TokenSimbolo.sdiv ||
               tokenAtual.getSimbolo() == TokenSimbolo.se) {
            TokenSimbolo op = tokenAtual.getSimbolo();
            proximoToken();
            String tipo2 = analisaFatorComTipo();
            if (op == TokenSimbolo.se) {
                if (!"booleano".equals(tipo) || !"booleano".equals(tipo2))
                    erro("Operador 'e' exige operandos booleanos");
                tipo = "booleano";
            } else {
                if (!"inteiro".equals(tipo) || !"inteiro".equals(tipo2))
                    erro("Operacao aritmetica exige operandos inteiros");
                tipo = "inteiro";
            }
        }
        return tipo;
    }

    private String analisaFatorComTipo() throws IOException {
        switch (tokenAtual.getSimbolo()) {
            case sidentificador:
                String nome = tokenAtual.getLexema();
                Simbolo s = tabela.buscar(nome);
                if (s == null)
                    erro("Identificador '" + nome + "' nao declarado");
                String tipo = s.getTipo();
                if ("procedimento".equals(tipo) || "programa".equals(tipo))
                    erro("Identificador '" + nome + "' do tipo '" + tipo + "' nao pode ser usado em expressao");
                proximoToken();
                return tipo;
            case snumero:
                proximoToken();
                return "inteiro";
            case sverdadeiro:
            case sfalso:
                proximoToken();
                return "booleano";
            case smais:
            case smenos:
                TokenSimbolo op = tokenAtual.getSimbolo();
                proximoToken();
                String t = analisaFatorComTipo();
                if (!"inteiro".equals(t))
                    erro("Operador unario '" + (op==TokenSimbolo.smais?"+":"-") + "' so pode ser aplicado a inteiros");
                return "inteiro";
            case snao:
                proximoToken();
                String t2 = analisaFatorComTipo();
                if (!"booleano".equals(t2))
                    erro("Operador 'nao' so pode ser aplicado a expressoes booleanas");
                return "booleano";
            case sabre_parenteses:
                proximoToken();
                String tipoExpr = analisaExpressaoComTipo();
                if (tokenAtual.getSimbolo() != TokenSimbolo.sfecha_parenteses)
                    erro("')' esperado");
                proximoToken();
                return tipoExpr;
            default:
                erro("Fator invalido");
                return null;
        }
    }

    // métodos sintáticos não tipados (mantidos como fallback)
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
                Simbolo s = tabela.buscar(tokenAtual.getLexema());
                if ("procedimento".equals(s.getTipo()) || "programa".equals(s.getTipo()))
                    erro("Identificador '" + tokenAtual.getLexema() + "' do tipo '" + s.getTipo() + "' nao pode ser usado aqui");
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
