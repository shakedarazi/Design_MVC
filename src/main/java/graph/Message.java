package graph;

import java.util.Arrays;
import java.util.Date;

public final class Message {
    public final byte[] data;
    public final String asText;
    public final double asDouble;
    public final Date date;

    public Message(byte[] data) {
        this(data, null, null);
    }

    public Message(String text) {
        this(null, text, null);
    }

    public Message(double value) {
        this(null, null, value);
    }

    private Message(byte[] data, String text, Double value) {
        if (data != null) {
            this.data = Arrays.copyOf(data, data.length);
            this.asText = new String(this.data);
            this.asDouble = parseDouble(this.asText);
        } else if (text != null) {
            this.asText = text;
            this.data = text.getBytes();
            this.asDouble = parseDouble(text);
        } else if (value != null) {
            this.asDouble = value;
            this.asText = Double.toString(value);
            this.data = this.asText.getBytes();
        } else {
            this.data = new byte[0];
            this.asText = "";
            this.asDouble = Double.NaN;
        }
        this.date = new Date();
    }

    private static double parseDouble(String text) {
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException ex) {
            return Double.NaN;
        }
    }
}
