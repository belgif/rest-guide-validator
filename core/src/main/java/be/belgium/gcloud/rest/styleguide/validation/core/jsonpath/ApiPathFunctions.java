package be.belgium.gcloud.rest.styleguide.validation.core.jsonpath;

import be.belgium.gcloud.rest.styleguide.validation.core.OperationEnum;
import com.jayway.jsonpath.Filter;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.jayway.jsonpath.Criteria.where;

/**
 * Json path function tool.
 */
public class ApiPathFunctions {
    public static final String DONT_HAVE_TPL ="$.paths[*].%s[?].operationId";
    public static final String PRODUCES_TPL = "$.paths[*].%s[?]['operationId','produces']";
    public static final String CONSUMES_TPL = "$.paths[*].%s[?]['operationId','consumes']";

    public static final String PRODUCES = "produces";
    public static final String CONSUMES = "consumes";
    public static final String OPERATIONID = "operationId";

    public static final String SWAGGER_JPATH = "$.swagger";

    // used in rules
    public static final String[] PRODUCES_MEDIATYPE = new String[]{"application/json", "application/problem+json"};
    public static final String[] CONSUMES_MEDIATYPE = new String[]{"application/json"};

    private ApiPathFunctions(){}

    static boolean isSwaggerV2(String jsonString){
        try {
            String swagger = JsonPath.read(jsonString, SWAGGER_JPATH);
            return swagger.startsWith("2.") ;
        } catch (PathNotFoundException e) {
            return false;
        }
    }

    /**
     * Get all path.operationId for the operation that not have a 'produces' field.
     * @param jsonString a valid swagger 2.x json string
     * @param operation
     * @return a list of operationId (String)
     */
    public static List<Object> operationIdDontHaveProduce(String jsonString, OperationEnum operation){
        if( ! isSwaggerV2(jsonString))
            return Collections.emptyList();

        Filter producesFilter = Filter.filter(
                where(PRODUCES).exists(false));
        return JsonPath.read(jsonString, String.format(DONT_HAVE_TPL, operation.label), producesFilter);
    }

    /**
     * Get all path.operationId for the operation that not have a 'consumes' field.
     * @param jsonString a valid swagger 2.x json string
     * @param operation
     * @return a list of operationId (String)
     */
    public static List<Object> operationIdDontHaveConsume(String jsonString, OperationEnum operation){
        if( ! isSwaggerV2(jsonString))
            return Collections.emptyList();

        Filter consumesFilter = Filter.filter(
                where(CONSUMES).exists(false));
        return JsonPath.read(jsonString, String.format(DONT_HAVE_TPL, operation.label), consumesFilter);
    }

    /**
     * Get all path.operationId for the operation that not have a 'produces' field value in the mediaType array.
     * @param jsonString a valid swagger 2.x json string
     * @param operation
     * @param mediaType
     * @return a list of operationId (String)
     */
    public static List<OperationData> operationDataDontHaveProduce(String jsonString, OperationEnum operation, String[] mediaType){
        if( ! isSwaggerV2(jsonString))
            return Collections.emptyList();

        Filter producesFilter = Filter.filter(
                where(PRODUCES).noneof(mediaType).and(PRODUCES).exists(true));
        List<Map<String, Object>> arr = JsonPath.read(jsonString, String.format(PRODUCES_TPL, operation.label), producesFilter);

        return arr.stream().map(map ->{
            String[] produces = ((List<String>) map.get(PRODUCES)).toArray(new String[0]);
            return OperationData.builder()
                    .operation(operation)
                    .operationId((String)map.get(OPERATIONID))
                    .produces(produces)
                    .build();
        }).collect(Collectors.toList());
    }

    /**
     * Get all path.operationId for the operation that not have a 'consumes' field value in the mediaType array.
     * @param jsonString a valid swagger 2.x json string
     * @param operation
     * @param mediaType
     * @return a list of operationId (String)
     */
    public static List<OperationData> operationDataDontHaveConsume(String jsonString, OperationEnum operation, String[] mediaType){
        if( ! isSwaggerV2(jsonString))
            return Collections.emptyList();

        Filter consumesFilter = Filter.filter(
                where(CONSUMES).noneof(mediaType).and(CONSUMES).exists(true));
        List<Map<String,Object>> arr = JsonPath.read(jsonString, String.format(CONSUMES_TPL, operation.label), consumesFilter);

        return arr.stream().map(map ->{
            String[] consumes = ((List<String>) map.get(CONSUMES)).toArray(new String[0]);
            return OperationData.builder()
                    .operation(operation)
                    .operationId((String)map.get(OPERATIONID))
                    .consumes(consumes)
                    .build();
        }).collect(Collectors.toList());
    }
}