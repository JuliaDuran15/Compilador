import java.util.ArrayList;
import java.util.List;

public class GeradorCodigo {

    private List<String> codigo = new ArrayList<>();

    public void gera(String rot, String inst, String arg1, String arg2) {

        String linha = "";

        if (rot != null && !rot.isEmpty())
            linha += rot + " ";   // ex: L1 NULL

        linha += inst;            // ex: CALL

        if (arg1 != null && !arg1.isEmpty())
            linha += " " + arg1;

        if (arg2 != null && !arg2.isEmpty())
            linha += " " + arg2;

        codigo.add(linha.trim());
    }

    public List<String> getCodigo() {
        return codigo;
    }

    public void imprimir() {
        for (String linha : codigo)
            System.out.println(linha);
    }
}
