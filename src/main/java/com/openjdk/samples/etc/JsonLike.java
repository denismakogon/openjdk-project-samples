package com.openjdk.samples.etc;

public class JsonLike {

    public class Inner {
        String a;
        boolean b;

        protected final String jsonPattern = """
                    {
                        "a": "%s",
                        "b": "%s"
                    }
                    """;

        public Inner(String a, boolean b) {
            this.a = a;
            this.b = b;
        }

        @Override
        public String toString() {
            return jsonPattern.formatted(a, b);
        }
    }
    String a;
    Integer b;
    Inner c;

    public JsonLike(String a, Integer b) {
        this.a = a;
        this.b = b;
        this.c = new Inner(a, true);
    }

    protected final String jsonPattern = """
            {
                "a": "%s",
                "b": "%s",
                "c": %s
            }
            """;

    @Override
    public String toString() {
        return jsonPattern.formatted(a, b, c);
    }


    public static void main(String[] args) {
        var jsonLike = new JsonLike("maybe a json", 8);
        System.out.println(jsonLike);
    }
}
