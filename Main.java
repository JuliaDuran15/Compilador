import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Uso: java Main <caminho_do_arquivo>");
            return;
        }
        String caminhoArquivo = args[0];
        try {
            AnalisadorLexico lexico = new AnalisadorLexico(caminhoArquivo);
            TabelaSimbolos tabela = new TabelaSimbolos();
            AnalisadorSintatico parser = new AnalisadorSintatico(lexico, tabela);
            parser.analisaPrograma();
            System.out.println("\nAnalise concluida. Tabela de Simbolos:");
            tabela.imprimir();
            lexico.fechar();
        } catch (IOException e) {
            System.err.println("Erro ao ler o arquivo: " + e.getMessage());
        } catch (RuntimeException e) {
            System.err.println(e.getMessage());
        }
    }
}