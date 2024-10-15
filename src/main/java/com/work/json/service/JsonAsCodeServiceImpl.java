package com.work.json.service;

import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.work.json.dto.FileDTO;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.HashMap;
import lombok.extern.slf4j.Slf4j;
import com.work.json.dto.InputDTO;
import org.springframework.util.StringUtils;

/**
 *
 * @author linux
 */
@Service
@Slf4j
public class JsonAsCodeServiceImpl implements JsonAsCodeService {

    private final ZipService zipService;

    private InputDTO request;

    private static final HashMap<String, FileDTO> mapFiles = new HashMap<>();

    // Expresión regular para validar el nombre de la variable
    private static final Pattern VALID_VARIABLE_NAME = Pattern.compile("^[a-z][a-zA-Z0-9]*$");

    // Expresión regular para nombres temporales permitidos
    private static final Pattern TEMP_VARIABLE_NAME = Pattern.compile("^[ijkmncd]$");

    public JsonAsCodeServiceImpl(ZipService zipService) {
        this.zipService = zipService;
    }

    @Override
    public void converter(InputDTO request) {
        try {
            this.request = request;

            List<FileDTO> files = jsonToClass(request);

            zipService.zip(files, request.getOutput());

        } catch (IOException e) {
            log.error("Error: ", e);
        }
    }

    private List<FileDTO> jsonToClass(InputDTO request) throws IOException {

        String className = request.getClassName();  // Nombre de la clase que será generada
        byte[] bytes = IOUtils.toByteArray(request.getInput().getInputStream());
        String json = new String(bytes);
        // Convertir el JSON a un JsonNode
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(json);
        // Generar el contenido de la clase Java        
        generateClassFromJson(rootNode, className);

        List<FileDTO> files = new ArrayList<>();
        mapFiles.forEach((k, v) -> files.add(v));
        log.info("Clases Java generadas y guardadas en archivos.");
        return files;
    }

    private boolean hasValidVariable(JsonNode rootNode) {
        Iterator<Map.Entry<String, JsonNode>> fields = rootNode.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String fieldName = field.getKey();
            if (!validateVariableName(fieldName, false)) {
                return false;
            }
        }
        return true;
    }

    private boolean hasArray(JsonNode rootNode) {
        Iterator<Map.Entry<String, JsonNode>> fields = rootNode.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            JsonNode node = field.getValue();
            if (node.isArray()) {
                return true;
            }
        }
        return false;
    }

    // Generar y guardar una clase Java a partir del JsonNode
    private void generateClassFromJson(JsonNode rootNode, String className) {
        StringBuilder classBuilder = new StringBuilder();

        if (StringUtils.hasText(this.request.getPackageName())) {
            classBuilder.append("package ").append(this.request.getPackageName()).append(";\n\n");
        }
        // Encabezado de la clase
        if (hasArray(rootNode)) {
            classBuilder.append("import java.util.List;\n");
        }
        if (!hasValidVariable(rootNode)) {
            classBuilder.append("import com.fasterxml.jackson.annotation.JsonProperty;\n\n");
        }

        classBuilder.append("public class ").append(className).append(" {\n\n");

        // Iterar sobre las propiedades del JSON y crear atributos
        Iterator<Map.Entry<String, JsonNode>> fields = rootNode.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String fieldName = field.getKey();
            JsonNode fieldValue = field.getValue();
            String fieldType = getJavaTypeFromJsonNode(fieldName, fieldValue, classBuilder);

            if (!validateVariableName(fieldName, false)) {
                classBuilder.append("    @JsonProperty(\"").append(fieldName).append("\")").append("\n");
                fieldName = fixVariableName(fieldName, false);
            }
            // Generar el atributo
            classBuilder.append("    private ").append(fieldType).append(" ").append(fieldName).append(";\n");
        }

        // Generar getters y setters
        fields = rootNode.fields();  // Reiniciar el iterador
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String fieldName = field.getKey();
            JsonNode fieldValue = field.getValue();
            String fieldType = getJavaTypeFromJsonNode(fieldName, fieldValue, classBuilder);
            if (!validateVariableName(fieldName, false)) {
                fieldName = fixVariableName(fieldName, false);
            }

            // Método getter
            classBuilder.append("\n    public ").append(fieldType).append(" get")
                    .append(capitalize(fieldName)).append("() {\n")
                    .append("        return ").append(fieldName).append(";\n")
                    .append("    }\n");

            // Método setter
            classBuilder.append("\n    public void set").append(capitalize(fieldName))
                    .append("(").append(fieldType).append(" ").append(fieldName).append(") {\n")
                    .append("        this.").append(fieldName).append(" = ").append(fieldName).append(";\n")
                    .append("    }\n");
        }

        // Cerrar la clase
        classBuilder.append("}\n");

        // Guardar la clase principal        
        addClassToFile(className, classBuilder.toString());

        // Reiniciar el iterador para guardar clases anidadas
        fields = rootNode.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String fieldName = field.getKey();
            JsonNode fieldValue = field.getValue();
            getJavaTypeFromJsonNode(fieldName, fieldValue, classBuilder); // Generar clases anidadas
        }

    }

    // Determinar el tipo de dato en Java a partir de un JsonNode
    private String getJavaTypeFromJsonNode(String fieldName, JsonNode node, StringBuilder classBuilder) {
        if (node.isArray()) {
            JsonNode firstElement = node.elements().next();
            if (firstElement.isObject()) {
                // Si el array contiene objetos, generar una nueva clase para ellos
                String nestedClassName = capitalize(fieldName) + "Item";
                generateClassFromJson(firstElement, nestedClassName); // Generar la clase y guardarla
                return "List<" + nestedClassName + ">";
            } else {
                // Si el array contiene tipos primitivos
                String elementType = getJavaTypeFromJsonNode(null, firstElement, classBuilder);
                return "List<" + elementType + ">";
            }
        } else if (node.isInt()) {
            return fieldName == null ? "Integer" : "int";
        } else if (node.isLong()) {
            return fieldName == null ? "Long" : "long";
        } else if (node.isBoolean()) {
            return fieldName == null ? "Boolean" : "boolean";
        } else if (node.isDouble()) {
            return fieldName == null ? "Double" : "double";
        } else if (node.isTextual()) {
            return "String";
        } else if (node.isObject()) {
            // Generar una clase interna para objetos anidados
            String nestedClassName = capitalize(fieldName);
            generateClassFromJson(node, nestedClassName); // Generar la clase y guardarla
            return nestedClassName;
        } else {
            return "Object";  // Caso general para otros tipos o estructuras más complejas
        }
    }

    // Capitalizar la primera letra de una cadena (para nombres de métodos)
    private String capitalize(String str) {
        if (str == null || str.length() == 0) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    // Guardar el contenido de la clase en un archivo .java
    private void addClassToFile(String className, String classContent) {
        if (!mapFiles.containsKey(className)) {
            mapFiles.put(className, new FileDTO(classContent.getBytes(), className + ".java"));
        }
    }

    private boolean validateVariableName(String variableName, boolean isTemporary) {
        // Verificar si el nombre es nulo o vacío
        if (variableName == null || variableName.isEmpty()) {
            return false;
        }

        // Evitar nombres que empiecen con _ o $
        if (variableName.startsWith("_") || variableName.startsWith("$")) {
            return false;
        }

        // Validar nombre de variables temporales
        if (isTemporary && TEMP_VARIABLE_NAME.matcher(variableName).matches()) {
            return true;
        }

        // Verificar que el nombre siga el formato camelCase
        if (!VALID_VARIABLE_NAME.matcher(variableName).matches()) {
            return false;
        }

        // Evitar nombres de una sola letra (excepto variables temporales)
        if (variableName.length() == 1 && !isTemporary) {
            return false;
        }

        // Verificar que el nombre sea significativo (evitar nombres cortos no temporales)
        if (variableName.length() < 2 && !isTemporary) {
            return false;
        }

        return true;
    }

    private String fixVariableName(String variableName, boolean isTemporary) {
        if (variableName == null || variableName.isEmpty()) {
            return "var"; // Nombre predeterminado si la entrada está vacía
        }

        // Eliminar caracteres no permitidos al inicio (_ o $)
        if (variableName.startsWith("_") || variableName.startsWith("$")) {
            variableName = variableName.substring(1);
        }

        // Si el nombre es temporal, y válido como tal, se retorna sin cambios
        if (isTemporary && TEMP_VARIABLE_NAME.matcher(variableName).matches()) {
            return variableName;
        }

        // Convertir el nombre a camelCase si no lo está
        variableName = convertToCamelCase(variableName);

        // Evitar nombres de una sola letra si no es temporal
        if (variableName.length() == 1 && !isTemporary) {
            variableName = "var" + variableName.toUpperCase();
        }

        // Si el nombre sigue siendo corto y no es temporal, añadir más contexto
        if (variableName.length() < 2 && !isTemporary) {
            variableName = "variable" + variableName.toUpperCase();
        }

        return variableName;
    }

    private String convertToCamelCase(String name) {
        String[] parts = name.split("[^a-zA-Z0-9]");
        StringBuilder camelCaseName = new StringBuilder(parts[0].toLowerCase());

        for (int i = 1; i < parts.length; i++) {
            String part = parts[i];
            if (!part.isEmpty()) {
                camelCaseName.append(Character.toUpperCase(part.charAt(0)));
                if (part.length() > 1) {
                    camelCaseName.append(part.substring(1).toLowerCase());
                }
            }
        }

        return camelCaseName.toString();
    }
}
