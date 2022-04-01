package be.belgium.gcloud.rest.styleguide.validation.core.jsonpath;

import be.belgium.gcloud.rest.styleguide.validation.core.OperationEnum;
import com.jayway.jsonpath.Filter;
import com.jayway.jsonpath.JsonPath;
import net.minidev.json.JSONArray;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.jayway.jsonpath.Criteria.where;

public class ApiPathFunctions {
    public static final String DONT_HAVE_TPL ="$.paths[*].%s[?].operationId";
    public static final String PRODUCES_TPL = "$.paths[*].%s[?]['operationId','produces']";
    public static final String CONSUMES_TPL = "$.paths[*].%s[?]['operationId','consumes']";

    public static final String PRODUCES = "produces";
    public static final String CONSUMES = "consumes";
    public static final String OPERATIONID = "operationId";

    //used in rules
    public static final String[] PRODUCES_MEDIATYPE = new String[]{"application/json", "application/problem+json"};
    public static final String[] CONSUMES_MEDIATYPE = new String[]{"application/json"};

    private ApiPathFunctions(){}

    public static List<Object> operationIdDontHaveProduce(String jsonString, OperationEnum operation){
        Filter producesFilter = Filter.filter(
                where(PRODUCES).exists(false));
        JSONArray arr = JsonPath.read(jsonString, String.format(DONT_HAVE_TPL, operation.label), producesFilter);
        return arr.stream().collect(Collectors.toList());
    }
    public static List<Object> operationIdDontHaveConsume(String jsonString, OperationEnum operation){
        Filter consumesFilter = Filter.filter(
                where(CONSUMES).exists(false));
        JSONArray arr = JsonPath.read(jsonString, String.format(DONT_HAVE_TPL, operation.label), consumesFilter);
        return arr.stream().collect(Collectors.toList());
    }

    public static List<OperationData> operationDataDontHaveProduce(String jsonString, OperationEnum operation, String[] mediaType){
        Filter producesFilter = Filter.filter(
                where(PRODUCES).noneof(mediaType).and(PRODUCES).exists(true));
        JSONArray arr = JsonPath.read(jsonString, String.format(PRODUCES_TPL, operation.label), producesFilter);

        return arr.stream().map(o ->{
            Map<String, Object> map = (Map) o;
            String[] produces = ((JSONArray) map.get(PRODUCES)).toArray(new String[0]);
            return OperationData.builder()
                    .operation(operation)
                    .operationId((String)map.get(OPERATIONID))
                    .produces(produces)
                    .build();
        }).collect(Collectors.toList());
    }
    public static List<OperationData> operationDataDontHaveConsume(String jsonString, OperationEnum operation, String[] mediaType){
        Filter consumesFilter = Filter.filter(
                where(CONSUMES).noneof(mediaType).and(CONSUMES).exists(true));
        JSONArray arr = JsonPath.read(jsonString, String.format(CONSUMES_TPL, operation.label), consumesFilter);

        return arr.stream().map(o ->{
            Map<String, Object> map = (Map) o;
            String[] consumes = ((JSONArray) map.get(CONSUMES)).toArray(new String[0]);
            return OperationData.builder()
                    .operation(operation)
                    .operationId((String)map.get(OPERATIONID))
                    .consumes(consumes)
                    .build();
        }).collect(Collectors.toList());
    }
}