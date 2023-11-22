package com.example.MSSQLConnection.controller;

import com.example.MSSQLConnection.concurrency.Worker;
import com.example.MSSQLConnection.model.Customer;
import com.example.MSSQLConnection.model.Greeting;
import com.example.MSSQLConnection.util.MemoryMapUtil;
import com.example.MSSQLConnection.util.TwilioUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jooq.tools.json.JSONParser;
import org.jooq.tools.json.ParseException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static junit.framework.Assert.assertTrue;

@RestController
public class TestController {

    @GetMapping("/test")
    public Map<String, Object> test() throws InterruptedException {
        List<String> outputScraper = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch countDownLatch = new CountDownLatch(5);
        List<Thread> workers = Stream
                .generate(() -> new Thread(new Worker(outputScraper, countDownLatch)))
                .limit(5)
                .collect(toList());

        workers.forEach(Thread::start);
        countDownLatch.await();
        outputScraper.add("Latch released");


        return Collections.singletonMap("message", "Test is completed!");
    }

    @GetMapping("/memoryMap")
    public Map<String, Object> memoryMap() {
        MemoryMapUtil.writeAndReadMemory();

        return Collections.singletonMap("message", "Memory Map example is completed!");
    }

    @GetMapping("/getDateTime")
    public Map<String, Object> getDateTime() {
        LocalDate localDateNow = LocalDate.now();
        LocalTime localTime = LocalTime.now();
        LocalDate localDateTomorrow = LocalDate.now().plusDays(1);

        Map<String, Object> result = new HashMap<>();
        result.put("Today", localDateNow);
        result.put("Now", localTime);
        result.put("Tomorrow", localDateTomorrow);

        return result;
    }

    @GetMapping("/reflection")
    public Map<String, Object> reflection() {
        Object greeting = new Greeting();
        Field[] fields = greeting.getClass().getDeclaredFields();

        List<String> actualFieldNames = getFieldNames(fields);

        assertTrue(Arrays.asList("id", "content").containsAll(actualFieldNames));

        return Collections.singletonMap("message", "Reflection method executed!");
    }

    @GetMapping("/getRandoms/{mod}")
    public Map<String, Integer> getRandoms(@PathVariable Integer mod) {
        return generator(0, mod, 3, 1, 10);
    }

    @GetMapping("/sendSMS")
    public String sendSMS() {
        TwilioUtil.sendSMS();
        return "SMS sent to user";
    }

    @GetMapping("/getCustomerData")
    public String getCustomer() throws IOException, ParseException {
        Map<String, Object> result = new HashMap<>();
        File file = new File("test.json");
        JSONParser parser = new JSONParser();
        Object obj = parser.parse(new FileReader(file));
        JSONArray jsonArray = new JSONArray(obj.toString());
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            Customer customer = new Customer();
            customer.setName((String) jsonObject.get("name"));
            customer.setCity((String) jsonObject.get("city"));
            customer.setJob((String) jsonObject.get("job"));
            result.put(customer.getName(), customer);
        }

        ObjectMapper objectMapper = new ObjectMapper();
        String jsonOutput = "";
        try {
            jsonOutput = objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return jsonOutput;
    }

    private static List<String> getFieldNames(Field[] fields) {
        List<String> fieldNames = new ArrayList<>();
        for (Field field : fields)
            fieldNames.add(field.getName());
        return fieldNames;
    }

    // Linear Congruential Generator
    static Map<String, Integer> generator(int seed, int mod, int multiplier,int inc, int noOfRandomNum)
    {
        int[] randomNums = new int[noOfRandomNum];
        Map<String, Integer> result = new HashMap<>();

        randomNums[0] = seed;
        result.put(String.valueOf(0), seed);

        for (int i = 1; i < noOfRandomNum; i++) {
            randomNums[i] = ((randomNums[i - 1] * multiplier) + inc) % mod;
            result.put(String.valueOf(i), randomNums[i]);
        }
        return result;
    }
}
