package assembler;

import java.util.List;

/**
 * Bir macro tanımını temsil eder: isim, parametreler ve gövde.
 */
public class Macro {
    private final String name;
    private final List<String> parameters;
    private final List<String> body;

    public Macro(String name, List<String> parameters, List<String> body) {
        this.name = name;
        this.parameters = parameters;
        this.body = body;
    }

    public String getName() {
        return name;
    }

    public List<String> getParameters() {
        return parameters;
    }

    public List<String> getBody() {
        return body;
    }
} 