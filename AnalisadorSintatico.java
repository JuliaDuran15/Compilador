import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AnalisadorSintatico {
    private Token tokenAtual;
    private TabelaSimbolos tabela;
    private AnalisadorLexico lexico;
    private GeradorCodigo gc = new GeradorCodigo();
    private int rotulo = 1;   // rótulos L1, L2, L3...
    private List<Token> saidaPos = new ArrayList<>();
    private List<Token> pilhaPos = new ArrayList<>();
    private List<Token> bufferExpr;


    private String novoRotulo() {
        return "L" + (rotulo++);
    }

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
        
        gc.gera("", "START", "", "");

        // reservar slot 0 do programa (retorno de função)
        gc.gera("", "ALLOC", "0", "1");

        // Garante que as variáveis globais comecem em 1 (slot 0 não é gerenciado pela tabela, mas ocupa memória)
        tabela.setEnderecoInicial(1);
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
            // Libera slot 0 do programa (retorno de função)
            gc.gera("", "DALLOC", "0", "1");

            gc.gera("", "HLT", "", "");

            proximoToken();
            System.out.println("Programa valido!");
            salvarCodigoGeradoEmArquivo();

        } else {
            erro("Ponto final esperado apos 'fim' do programa principal");
        }
    }

    private void analisaBloco() throws IOException {
        int inicioEscopo = tabela.getNivelAtual();
        int enderecoAntes = tabela.getEnderecoAtual();
        
        tabela.entrarEscopo();
        analisaEtVariaveis();

            //// >>> ALTERAÇÃO: ALLOC (variáveis do escopo)
        int depois = tabela.getEnderecoAtual();
        int nVars = depois - enderecoAntes;
        if (nVars > 0)
            gc.gera("", "ALLOC", enderecoAntes + "", nVars + "");
            
        analisaSubrotinas();
        analisaComandos();

            //// >>> ALTERAÇÃO: desalocar variáveis locais
        if (nVars > 0)
            gc.gera("", "DALLOC", enderecoAntes + "", nVars + "");
        
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
        int flag = 0;
        String auxrot = null;
        
        if (tokenAtual.getSimbolo() == TokenSimbolo.sprocedimento ||
            tokenAtual.getSimbolo() == TokenSimbolo.sfuncao) {

            auxrot = novoRotulo();

            // GERA("", JMP, auxrot, "")
            gc.gera("", "JMP", auxrot, "");

            flag = 1;
        }


        while (tokenAtual.getSimbolo() == TokenSimbolo.sprocedimento || tokenAtual.getSimbolo() == TokenSimbolo.sfuncao) {
            if (tokenAtual.getSimbolo() == TokenSimbolo.sprocedimento)
                analisaDeclaracaoProcedimento();
            else
                analisaDeclaracaoFuncao();

            if (tokenAtual.getSimbolo() != TokenSimbolo.sponto_virgula)
                erro("Ponto e virgula esperado apos declaracao de sub-rotina");
            proximoToken();
        }

        if (flag == 1) {
         // início do principal → Lx: NULL
            gc.gera(auxrot, "NULL", "", "");
        }
    }

    private void analisaDeclaracaoProcedimento() throws IOException {
        proximoToken();
        if (tokenAtual.getSimbolo() != TokenSimbolo.sidentificador)
            erro("Identificador esperado apos 'procedimento'");

        String nome = tokenAtual.getLexema();

        if (tabela.buscar(nome) != null)
            erro("Procedimento '" + nome + "' ja declarado (identificador visivel com mesmo nome)");
        
        int rot = rotulo++;

        if (!tabela.inserir(nome, tabela.getNivelAtual(), "procedimento"))
            erro("Nao foi possivel inserir procedimento '" + nome + "'");

        Simbolo s = tabela.buscar(nome);
        s.setEndereco(rot);

        // gera rótulo da subrotina
        gc.gera("L" + rot, "NULL", "", "");
        proximoToken();

        if (tokenAtual.getSimbolo() != TokenSimbolo.sponto_virgula)
            erro("Ponto e virgula esperado apos cabecalho de procedimento");

        proximoToken();
        analisaBloco();

        //// >>> ALTERAÇÃO: fim de procedimento
        gc.gera("", "RETURN", "", "");
    }

    private void analisaDeclaracaoFuncao() throws IOException {

        // consome 'funcao'
        proximoToken();
        if (tokenAtual.getSimbolo() != TokenSimbolo.sidentificador)
            erro("Identificador esperado apos 'funcao'");

        String nome = tokenAtual.getLexema();

        if (tabela.buscar(nome) != null)
            erro("Funcao '" + nome + "' já declarada");

        // consome nome
        proximoToken();

        if (tokenAtual.getSimbolo() != TokenSimbolo.sdois_pontos)
            erro("':' esperado apos nome da funcao");

        // consome ':'
        proximoToken();

        // -----------------------------
        // Determinação do tipo da função
        // -----------------------------
        String tipoFuncao;

        if (tokenAtual.getSimbolo() == TokenSimbolo.sinteiro) {
            tipoFuncao = "funcao_inteiro";
        }
        else if (tokenAtual.getSimbolo() == TokenSimbolo.sbooleano) {
            tipoFuncao = "funcao_booleano";
        }
        else {
            erro("Tipo invalido de retorno na funcao");
            return; // apenas para evitar warning
        }

        // consome tipo
        proximoToken();

        // cria rótulo (Lx)
        String rotuloFuncao = novoRotulo();

        // insere símbolo
        if (!tabela.inserir(nome, tabela.getNivelAtual(), tipoFuncao))
            erro("Nao foi possivel inserir funcao '" + nome + "'");

        // salva destino do CALL
        tabela.buscar(nome).setEndereco(Integer.parseInt(rotuloFuncao.substring(1)));

        if (tokenAtual.getSimbolo() != TokenSimbolo.sponto_virgula)
            erro("Ponto e virgula esperado apos declaracao da funcao");

        // consome ';'
        proximoToken();

        //-------------------------------
        // Rótulo da função
        //-------------------------------
        gc.gera(rotuloFuncao, "NULL", "", "");

        //-------------------------------
        // Bloco da função
        //-------------------------------
        analisaBloco();  // já faz ALLOC/DALLOC automaticamente

        //-------------------------------
        // Return da função
        //-------------------------------
        gc.gera("", "RETURN", "", "");
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

        String tipo = s.getTipo();
        int rot = s.getEndereco();     // para funções/procedimentos

        proximoToken();

        if (tokenAtual.getSimbolo() == TokenSimbolo.sdois_pontos)
            erro("Use ':=' para atribuicao. ':' isolado e invalido.");

        // ------------------------------
        // ATRIBUIÇÃO
        // ------------------------------
        if (tokenAtual.getSimbolo() == TokenSimbolo.satribuicao) {

            if (!tipo.equals("inteiro") && !tipo.equals("booleano") && !tipo.equals("funcao_inteiro") && !tipo.equals("funcao_booleano"))
                erro("Atribuicao somente para variaveis ou funcoes.");

            proximoToken();

            // LIMPA as listas antes de analisar a expressão
            saidaPos.clear();
            pilhaPos.clear();

            String tipoExpr = analisaExpressaoComTipo();

            String tipoEsperado = tipo;
            if (tipo.equals("funcao_inteiro")) tipoEsperado = "inteiro";
            if (tipo.equals("funcao_booleano")) tipoEsperado = "booleano";

            if (!tipoEsperado.equals(tipoExpr))
                erro("Tipos incompativeis na atribuicao.");

            geraCodigoPosFixa();

            if (tipo.startsWith("funcao")) {
                gc.gera("", "STR", "0", "");  // Funções retornam em M[0]
            } else {
                gc.gera("", "STR", s.getEndereco()+"", "");
            }
            return;
        }

        // ------------------------------
        // CHAMADA DE PROCEDIMENTO
        // ------------------------------
        if (tipo.equals("procedimento")) {
            gc.gera("", "CALL", "L" + rot, "");
            return;
        }

        // ------------------------------
        // CHAMADA DE FUNÇÃO COMO COMANDO
        // ------------------------------
        if (tipo.equals("funcao_inteiro") || tipo.equals("funcao_booleano")) {
            gc.gera("", "CALL", "L" + rot, "");
            return;  // valor fica em M[0]
        }

        // ------------------------------
        // VARIÁVEL SOZINHA → ERRO
        // ------------------------------
        erro("Variavel '" + lex + "' usada como comando. Faltou ':='?");
    }


    private void analisaSe() throws IOException {
        proximoToken();
        
        // LIMPA as listas
        saidaPos.clear();
        pilhaPos.clear();
        
        String tipoExpr = analisaExpressaoComTipo();
        if (!"booleano".equals(tipoExpr))
            erro("Expressao do 'se' deve ser booleana");
        
        // GERA código da expressão ANTES do JMPF
        geraCodigoPosFixa();
        
        if (tokenAtual.getSimbolo() != TokenSimbolo.sentao)
            erro("'entao' esperado");
        proximoToken();

        String Lelse = novoRotulo();
        String Lfim = novoRotulo();

        gc.gera("", "JMPF", Lelse, "");

        analisaComando();
        if (tokenAtual.getSimbolo() == TokenSimbolo.ssenao) {
            proximoToken();
            gc.gera("", "JMP", Lfim, "");
            gc.gera(Lelse, "NULL", "", "");
            analisaComando();
            gc.gera(Lfim, "NULL", "", "");
        } else {
            gc.gera(Lelse, "NULL", "", "");
        }
    }

    private void analisaEnquanto() throws IOException {
        String L1 = novoRotulo();
        gc.gera(L1, "NULL", "", "");
        
        proximoToken();
        
        // LIMPA as listas
        saidaPos.clear();
        pilhaPos.clear();
        
        String tipoExpr = analisaExpressaoComTipo();
        if (!"booleano".equals(tipoExpr))
            erro("Expressao do 'enquanto' deve ser booleana");
        
        // GERA código da expressão ANTES do JMPF
        geraCodigoPosFixa();
        
        String L2 = novoRotulo();
        gc.gera("", "JMPF", L2, "");

        if (tokenAtual.getSimbolo() != TokenSimbolo.sfaca)
            erro("'faca' esperado apos expressao do 'enquanto'");
        proximoToken();
        analisaComando();

        gc.gera("", "JMP", L1, "");
        gc.gera(L2, "NULL", "", "");
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

            //// >>> GERAR RD + STR
        gc.gera("", "RD", "", "");
        gc.gera("", "STR", s.getEndereco()+"", "");

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
        //// >>> GERAR LDV + PRN
        gc.gera("", "LDV", s.getEndereco()+"", "");
        gc.gera("", "PRN", "", "");
        proximoToken();
        if (tokenAtual.getSimbolo() != TokenSimbolo.sfecha_parenteses)
            erro("')' esperado apos identificador em 'escreva'");
        proximoToken();
    }

    private String analisaExpressaoComTipo() throws IOException {

        String tipo1 = analisaExpressaoSimplesComTipo();

        if (ehRelacional(tokenAtual.getSimbolo())) {

            Token operador = tokenAtual;
            TokenSimbolo op = operador.getSimbolo();

            proximoToken();

            String tipo2 = analisaExpressaoSimplesComTipo();

            if (!tipo1.equals(tipo2))
                erro("Incompatibilidade de tipos em comparacao: " +
                    tipo1 + " " + op + " " + tipo2);

            saidaPos.add(operador);

            return "booleano";
        }

        return tipo1;
    }


    private String analisaExpressaoSimplesComTipo() throws IOException {

        // ---------------------------------------------
        // Caso comece com operador unário + ou -
        // ---------------------------------------------
        if (tokenAtual.getSimbolo() == TokenSimbolo.smais ||
            tokenAtual.getSimbolo() == TokenSimbolo.smenos) {

            Token operadorUnario = tokenAtual;  // salva antes
            TokenSimbolo sinal = tokenAtual.getSimbolo();

            proximoToken();

            String tipo = analisaTermoComTipo();

            if (!"inteiro".equals(tipo))
                erro("Operador unario '" +
                        (sinal == TokenSimbolo.smais ? "+" : "-") +
                        "' so pode ser aplicado a inteiros");

            // adiciona o operador unário na pós-fixa
            saidaPos.add(operadorUnario);

            // continua analisando expressões normais após o termo
            while (tokenAtual.getSimbolo() == TokenSimbolo.smais ||
                tokenAtual.getSimbolo() == TokenSimbolo.smenos ||
                tokenAtual.getSimbolo() == TokenSimbolo.sou) {

                Token operador = tokenAtual;
                TokenSimbolo op = operador.getSimbolo();
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

                saidaPos.add(operador);
            }

            return tipo;
        }

        // ---------------------------------------------
        // Caso NÃO comece com + ou -
        // ---------------------------------------------
        String tipo = analisaTermoComTipo();

        while (tokenAtual.getSimbolo() == TokenSimbolo.smais ||
            tokenAtual.getSimbolo() == TokenSimbolo.smenos ||
            tokenAtual.getSimbolo() == TokenSimbolo.sou) {

            Token operador = tokenAtual;
            TokenSimbolo op = operador.getSimbolo();
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

            saidaPos.add(operador);
        }

        return tipo;
    }


    private String analisaTermoComTipo() throws IOException {
        String tipo = analisaFatorComTipo();
        while (tokenAtual.getSimbolo() == TokenSimbolo.smultiplicacao ||
               tokenAtual.getSimbolo() == TokenSimbolo.sdiv ||
               tokenAtual.getSimbolo() == TokenSimbolo.se) {
            TokenSimbolo op = tokenAtual.getSimbolo();
            Token operador = tokenAtual;      // salva antes de avançar

            proximoToken();
            String tipo2 = analisaFatorComTipo();
            if (op == TokenSimbolo.se) {
                if (!"booleano".equals(tipo) || !"booleano".equals(tipo2))
                    erro("Operador 'e' exige operandos booleanos");

                saidaPos.add(operador);

                tipo = "booleano";
            } else {
                if (!"inteiro".equals(tipo) || !"inteiro".equals(tipo2))
                    erro("Operacao aritmetica exige operandos inteiros");

                saidaPos.add(operador);

                tipo = "inteiro";
            }
        }
        return tipo;
    }

    private String analisaFatorComTipo() throws IOException {

        TokenSimbolo simb = tokenAtual.getSimbolo();

        switch (simb) {

            // ------------------------------------------
            // IDENTIFICADOR
            // ------------------------------------------
            case sidentificador: {

                String nome = tokenAtual.getLexema();
                Simbolo s = tabela.buscar(nome);

                // não declarado
                if (s == null)
                    erro("Identificador '" + nome + "' nao declarado");

                String tipo = s.getTipo();

                // procedimento não pode ser usado em expressão
                if (tipo.equals("procedimento"))
                    erro("Procedimento '" + nome + "' nao pode ser usado em expressao");

                // programa nunca pode ser fator
                if (tipo.equals("programa"))
                    erro("Programa nao pode ser usado em expressao");

                // tipo inválido
                if (!tipo.equals("inteiro") &&
                    !tipo.equals("booleano") &&
                    !tipo.equals("funcao_inteiro") &&
                    !tipo.equals("funcao_booleano"))
                    erro("Identificador '" + nome + "' com tipo invalido: " + tipo);

                // Adiciona à pós-fixa (variável ou função)
                saidaPos.add(tokenAtual);

                proximoToken();

                // se for função → fator deve retornar o tipo base
                if (tipo.equals("funcao_inteiro")) return "inteiro";
                if (tipo.equals("funcao_booleano")) return "booleano";

                return tipo; // variável normal
            }

            // ------------------------------------------
            // CONSTANTES NUMÉRICAS E BOOLEANAS
            // ------------------------------------------
            case snumero:
                saidaPos.add(tokenAtual);
                proximoToken();
                return "inteiro";

            case sverdadeiro:
            case sfalso:
                saidaPos.add(tokenAtual);
                proximoToken();
                return "booleano";

            // ------------------------------------------
            // OPERADORES UNÁRIOS
            // ------------------------------------------
            case smais:
            case smenos: {
                Token op = tokenAtual;
                proximoToken();
                String t = analisaFatorComTipo();
                if (!"inteiro".equals(t))
                    erro("Operador unario '" + (simb==TokenSimbolo.smais?"+":"-") + "' so pode ser aplicado a inteiros");

                // operador unário também vai pra pós-fixa
                saidaPos.add(op);
                return "inteiro";
            }

            case snao: {
                Token op = tokenAtual;
                proximoToken();
                String t = analisaFatorComTipo();
                if (!"booleano".equals(t))
                    erro("Operador 'nao' so pode ser aplicado a expressoes booleanas");

                saidaPos.add(op);
                return "booleano";
            }

            // ------------------------------------------
            // PARÊNTESES
            // ------------------------------------------
            case sabre_parenteses:
                proximoToken();
                String tipoExpr = analisaExpressaoComTipo();
                if (tokenAtual.getSimbolo() != TokenSimbolo.sfecha_parenteses)
                    erro("')' esperado");
                proximoToken();
                return tipoExpr;

            // ------------------------------------------
            // ERRO GERAL
            // ------------------------------------------
            default:
                erro("Fator invalido: " + tokenAtual.getLexema());
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


        //// >>> FIM NOVA FUNÇÃO
    private void salvarCodigoGeradoEmArquivo() {
        try {
            java.io.PrintWriter writer =
                new java.io.PrintWriter("codigo_mvd.obj", "UTF-8");

            for (String linha : gc.getCodigo()) {
                writer.println(linha);
            }

            writer.close();
            System.out.println("Arquivo 'codigo_mvd.obj' gerado com sucesso!");

        } catch (Exception e) {
            System.out.println("Erro ao salvar arquivo de código: " + e.getMessage());
        }
    }

    private void converteExpressaoParaPosFixa() throws IOException {

        // GUARDA estado anterior
        Token salvo = tokenAtual;

        saidaPos.clear();
        pilhaPos.clear();

        analisaExpressaoPos();  // lê apenas expressão
        desempilhaPilhaPos();

        // RESTAURA para que análise sintática continue normalmente
        tokenAtual = salvo;
    }

    private void analisaExpressaoPos() throws IOException {
        analisaExpressaoSimplesPos();
        if (ehRelacional(tokenAtual.getSimbolo())) {
            Token operador = tokenAtual;
            proximoToken();
            analisaExpressaoSimplesPos();
            empilhaOperadorPos(operador);
        }
    }
    private void analisaExpressaoSimplesPos() throws IOException {
        if (tokenAtual.getSimbolo() == TokenSimbolo.smais ||
            tokenAtual.getSimbolo() == TokenSimbolo.smenos) {

            Token operador = tokenAtual;
            proximoToken();
            analisaTermoPos();
            empilhaOperadorPos(operador);
            return;
        }

        analisaTermoPos();

        while (tokenAtual.getSimbolo() == TokenSimbolo.smais ||
            tokenAtual.getSimbolo() == TokenSimbolo.smenos ||
            tokenAtual.getSimbolo() == TokenSimbolo.sou) {

            Token operador = tokenAtual;
            proximoToken();
            analisaTermoPos();
            empilhaOperadorPos(operador);
        }
    }
    private void analisaTermoPos() throws IOException {
        analisaFatorPos();

        while (tokenAtual.getSimbolo() == TokenSimbolo.smultiplicacao ||
            tokenAtual.getSimbolo() == TokenSimbolo.sdiv ||
            tokenAtual.getSimbolo() == TokenSimbolo.se) {

            Token operador = tokenAtual;
            proximoToken();
            analisaFatorPos();
            empilhaOperadorPos(operador);
        }
    }
    private void analisaFatorPos() throws IOException {

        switch (tokenAtual.getSimbolo()) {

            case sidentificador:
            case snumero:
            case sverdadeiro:
            case sfalso:
                saidaPos.add(tokenAtual);
                proximoToken();
                break;

            case sabre_parenteses:
                pilhaPos.add(tokenAtual); // "("
                proximoToken();
                analisaExpressaoPos();
                if (tokenAtual.getSimbolo() != TokenSimbolo.sfecha_parenteses)
                    erro("')' esperado");
                proximoToken();
                desempilhaAteAbreParenteses();
                break;

            case snao:
                Token op = tokenAtual;
                proximoToken();
                analisaFatorPos();
                empilhaOperadorPos(op);
                break;

            default:
                erro("Fator inválido na pós-fixa: " + tokenAtual.getLexema());
        }
    }
    private boolean ehRelacional(TokenSimbolo s) {
    return s == TokenSimbolo.sigual ||
           s == TokenSimbolo.sdiferente ||
           s == TokenSimbolo.smaior ||
           s == TokenSimbolo.smenor ||
           s == TokenSimbolo.smaior_ig ||
           s == TokenSimbolo.smenor_ig;
}


    private int precedencia(TokenSimbolo s) {
        if (s == TokenSimbolo.smultiplicacao || s == TokenSimbolo.sdiv) return 6;
        if (s == TokenSimbolo.smais || s == TokenSimbolo.smenos) return 5;
        if (ehRelacional(s)) return 4;
        if (s == TokenSimbolo.sse) return 2;
        if (s == TokenSimbolo.sou) return 1;
        if (s == TokenSimbolo.snao) return 7;
        return 0;
    }

    private void empilhaOperadorPos(Token operador) {
        while (!pilhaPos.isEmpty()) {
            Token topo = pilhaPos.get(pilhaPos.size() - 1);
            if (precedencia(topo.getSimbolo()) >= precedencia(operador.getSimbolo())) {
                saidaPos.add(topo);
                pilhaPos.remove(pilhaPos.size() - 1);
            } else break;
        }
        pilhaPos.add(operador);
    }
    private void desempilhaAteAbreParenteses() {
        while (!pilhaPos.isEmpty()) {
            Token t = pilhaPos.remove(pilhaPos.size() - 1);
            if (t.getSimbolo() == TokenSimbolo.sabre_parenteses)
                break;
            saidaPos.add(t);
        }
    }
    private void desempilhaPilhaPos() {
        while (!pilhaPos.isEmpty()) {
            saidaPos.add(pilhaPos.remove(pilhaPos.size() - 1));
        }
        }
    private void geraCodigoPosFixa() {
        System.out.println("=== Gerando código pós-fixa ===");
        System.out.println("saidaPos tem " + saidaPos.size() + " elementos:");
        for (Token t : saidaPos) {
            System.out.println("  " + t.getLexema() + " (" + t.getSimbolo() + ")");
        }

        for (Token t : saidaPos) {

            TokenSimbolo s = t.getSimbolo();

            if (s == TokenSimbolo.sidentificador) {
                Simbolo sim = tabela.buscar(t.getLexema());

                // função
                if (sim.getTipo().startsWith("funcao")) {
                    gc.gera("", "CALL", "L" + sim.getEndereco(), "");
                    gc.gera("", "LDV", "0", "");
                }
                // variável
                else {
                    gc.gera("", "LDV", sim.getEndereco() + "", "");
                }

                continue;
            }

            if (s == TokenSimbolo.snumero) {
                gc.gera("", "LDC", t.getLexema(), "");
                continue;
            }

            switch (s) {
                case smais: gc.gera("", "ADD", "", ""); break;
                case smenos: gc.gera("", "SUB", "", ""); break;
                case smultiplicacao: gc.gera("", "MULT", "", ""); break;
                case sdiv: gc.gera("", "DIVI", "", ""); break;
                case se: gc.gera("", "AND", "", ""); break;
                case sou: gc.gera("", "OR", "", ""); break;
                case snao: gc.gera("", "NEG", "", ""); break;

                case smaior: gc.gera("", "CMA", "", ""); break;
                case smenor: gc.gera("", "CME", "", ""); break;
                case smaior_ig: gc.gera("", "CMAQ", "", ""); break;
                case smenor_ig: gc.gera("", "CMEQ", "", ""); break;
                case sigual: gc.gera("", "CEQ", "", ""); break;
                case sdiferente: gc.gera("", "CDIF", "", ""); break;
            }
        }
    }



}
