package com.example.tcpclient;

import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Slider;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class HelloController implements Initializable {
    public BorderPane borderPane;

    public Label label_temperature;
    public ProgressBar progressBar_temperature;
    public Label label_humidity;
    public ProgressBar progressBar_humidity;

    public Circle red_led;
    public Circle green_led;
    public Circle blue_led;

    public Button button_red_led;
    public Button button_green_led;
    public Button button_blue_led;
    public Button button_record;
    public Button button_play;
    
    public Slider red_slider;
    public Slider green_slider;
    public Slider blue_slider;

    public HelloController() {
        this.state_of_button_green_led = false;
        this.state_of_button_red_led = false;
        this.state_of_button_blue_led = false;
        this.socket = new Socket();
    }

    private boolean state_of_button_red_led;
    private boolean state_of_button_green_led;
    private boolean state_of_button_blue_led;
    private boolean isRecording = false;

    private long time = 0;
    private KeyCode prev_key = null;

    List<Buzzer> list = new ArrayList<Buzzer>();

    /* @Param resourceBundle */
    private final Socket socket;

    Connection conn = null;
    PreparedStatement ps = null;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            this.socket.connect(new InetSocketAddress("192.168.0.2", 9999));
            this.received_data_from_server(new ActionEvent());
        } catch (IOException e) {
//          throw new RuntimeException(e);
            System.out.printf("%s\r\n", e.getMessage());
            System.out.printf("%s\r\n", "서버 통신에 실패했습니다.");
        }
    }

    private double change_progressBar_value(double x, double in_min, double in_max,
                                            double out_min, double out_max) {
        return (x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min;
    }

    /*  Thread를 사용해서 서버에서 넘어오는 데이터를 연속으로 처리
        @param actionEvent  */
    private void received_data_from_server(ActionEvent actionEvent) {
        Thread thread_of_receiving = new Thread(()->{

            while(true) {
                try {
                    byte[] bytes_data = new byte[512];  // BUFSIZ : 512
                    // 소켓에서 들어오는 데이터
                    final InputStream input_Stream = this.socket.getInputStream();
                    final int read_byte_count = input_Stream.read(bytes_data);
                    if(read_byte_count ==  -1) {
                        throw new IOException();
                    }
                    final String[] parsing_data = new String(bytes_data, 0,
                            read_byte_count).trim().split(",");
                    if(parsing_data.length != 2) continue;
                    final double temperature = Double.parseDouble(parsing_data[0]);
                    final double humidity = Double.parseDouble(parsing_data[1]);
                    final double changed_temperature_for_progressBar
                            = this.change_progressBar_value(temperature, -10.0, 40.0, 0.0, 1.0);
                    final double changed_humidity_for_progressBar
                            = this.change_progressBar_value(humidity, 0.0, 100.0, 0.0, 1.0);
                    /*  출력값을 ProgressBar에 넣어 줘야하지만
                        ProgressBar는 thread이기 때문에 thread에서 접근 불가
                        (thread->thread로 그냥 접근하면 unsafe)
                     */
                    Platform.runLater(()-> {
                        progressBar_temperature.setProgress(changed_temperature_for_progressBar);
                        progressBar_humidity.setProgress(changed_humidity_for_progressBar);
                        label_temperature.setText(parsing_data[0]);
                        label_humidity.setText(parsing_data[1]);
                    });
                } catch (IOException e) {
                    System.out.printf("%s\r\n", e.getMessage());
                    System.out.printf("%s\r\n", "데이터 입력 에러");
                    try {
                        this.socket.close();
                    } catch (IOException ex) {
                        System.out.printf("%s\r\n", ex.getMessage());
                        System.out.printf("%s\r\n", "소켓 close() 에러");
                    }
                }
            }
        });
        thread_of_receiving.start();
    }

    public void buttonOnclickedRedLED(ActionEvent event) {
        System.out.printf("%s\r\n", "red button clicked");
        if(this.socket.isConnected()) {
            this.state_of_button_red_led ^= true;
            if(this.state_of_button_red_led) {
                this.red_led.setVisible(true);
                this.red_led.setFill(Paint.valueOf("red"));
                try {
                    String send_str = "RED_LED_" + (int)red_slider.getValue() + "\n";
                    byte[] byte_data = send_str.getBytes("UTF-8");
                    final var output_stream = this.socket.getOutputStream();
                    output_stream.write(byte_data);
                    output_stream.flush();
                } catch (UnsupportedEncodingException e) {
                    System.out.printf("%s\r\n", e.getMessage());
                    System.out.printf("%s\r\n", "인코딩 에러");
                } catch (IOException e) {
                    System.out.printf("%s\r\n", e.getMessage());
                    System.out.printf("%s\r\n", "OutputStream() 에러");
                }
                this.button_red_led.setText("RED LED ON");
            } else {
                this.red_led.setFill(Paint.valueOf("#9f4f4f"));
                try {
                    byte[] byte_data = ("RED_LED_0\n").getBytes("UTF-8");
                    final OutputStream output_stream = this.socket.getOutputStream();
                    output_stream.write(byte_data);
                    output_stream.flush();
                } catch (UnsupportedEncodingException e) {
                    System.out.printf("%s\r\n", e.getMessage());
                    System.out.printf("%s\r\n", "인코딩 에러");
                } catch (IOException e) {
                    System.out.printf("%s\r\n", e.getMessage());
                    System.out.printf("%s\r\n", "OutputStream() 에러");
                }
                this.button_red_led.setText("RED LED OFF");
            }
        }
    }

    public void buttonOnclickedGreenLED(ActionEvent event) {
        System.out.printf("%s\r\n", "green button clicked");
        if(this.socket.isConnected()) {
            this.state_of_button_green_led ^= true;
            if(this.state_of_button_green_led) {
                this.green_led.setVisible(true);
                this.green_led.setFill(Paint.valueOf("#2fff2f"));
                try {
                    String send_str = "GREEN_LED_" + (int)green_slider.getValue() + "\n";
                    byte[] byte_data = send_str.getBytes("UTF-8");
                    final var output_stream = this.socket.getOutputStream();
                    output_stream.write(byte_data);
                    output_stream.flush();
                } catch (UnsupportedEncodingException e) {
                    System.out.printf("%s\r\n", e.getMessage());
                    System.out.printf("%s\r\n", "인코딩 에러");
                } catch (IOException e) {
                    System.out.printf("%s\r\n", e.getMessage());
                    System.out.printf("%s\r\n", "OutputStream() 에러");
                }
                this.button_green_led.setText("GREEN LED ON");
            } else {
                this.green_led.setFill(Paint.valueOf("#4f9f4f"));
                try {
                    byte[] byte_data = ("GREEN_LED_0\n").getBytes("UTF-8");
                    final OutputStream output_stream = this.socket.getOutputStream();
                    output_stream.write(byte_data);
                    output_stream.flush();
                } catch (UnsupportedEncodingException e) {
                    System.out.printf("%s\r\n", e.getMessage());
                    System.out.printf("%s\r\n", "인코딩 에러");
                } catch (IOException e) {
                    System.out.printf("%s\r\n", e.getMessage());
                    System.out.printf("%s\r\n", "OutputStream() 에러");
                }
                this.button_green_led.setText("GREEN LED OFF");
            }
        }
    }

    public void buttonOnclickedBlueLED(ActionEvent actionEvent) {
        System.out.printf("%s\r\n", "blue button clicked");
        if(this.socket.isConnected()) {
            this.state_of_button_blue_led ^= true;
            if(this.state_of_button_blue_led) {
                this.blue_led.setVisible(true);
                this.blue_led.setFill(Paint.valueOf("#2f2fff"));
                try {
                    String send_str = "BLUE_LED_" + (int)blue_slider.getValue() + "\n";
                    byte[] byte_data = send_str.getBytes("UTF-8");
                    final var output_stream = this.socket.getOutputStream();
                    output_stream.write(byte_data);
                    output_stream.flush();
                } catch (UnsupportedEncodingException e) {
                    System.out.printf("%s\r\n", e.getMessage());
                    System.out.printf("%s\r\n", "인코딩 에러");
                } catch (IOException e) {
                    System.out.printf("%s\r\n", e.getMessage());
                    System.out.printf("%s\r\n", "OutputStream() 에러");
                }
                this.button_blue_led.setText("BLUE LED ON");
            } else {
                this.blue_led.setFill(Paint.valueOf("#6f6f9f"));
                try {
                    byte[] byte_data = ("BLUE_LED_0\n").getBytes("UTF-8");
                    final OutputStream output_stream = this.socket.getOutputStream();
                    output_stream.write(byte_data);
                    output_stream.flush();
                } catch (UnsupportedEncodingException e) {
                    System.out.printf("%s\r\n", e.getMessage());
                    System.out.printf("%s\r\n", "인코딩 에러");
                } catch (IOException e) {
                    System.out.printf("%s\r\n", e.getMessage());
                    System.out.printf("%s\r\n", "OutputStream() 에러");
                }
                this.button_blue_led.setText("BLUE LED OFF");
            }
        }
    }

    public void ctrl_led(Slider slider, String color) {
        if(socket.isConnected() && (state_of_button_red_led && color.equals("RED")) ||
                    (state_of_button_green_led && color.equals("GREEN")) ||
                    (state_of_button_blue_led && color.equals("BLUE"))) {
            try {
                String send_str = color + "_LED_" + (int)slider.getValue() + "\n";
                byte[] byte_data = send_str.getBytes("UTF-8");
                final var output_stream = this.socket.getOutputStream();
                output_stream.write(byte_data);
                output_stream.flush();
            } catch (UnsupportedEncodingException e) {
                System.out.printf("%s\r\n", e.getMessage());
                System.out.printf("%s\r\n", "인코딩 에러");
            } catch (IOException e) {
                System.out.printf("%s\r\n", e.getMessage());
                System.out.printf("%s\r\n", "OutputStream() 에러");
            }

            switch (color) {
                case "RED": button_red_led.setText("RED LED " + (int)slider.getValue()); break;
                case "GREEN": button_green_led.setText("GREEN LED " + (int)slider.getValue()); break;
                default: button_blue_led.setText("BLUE LED " + (int)slider.getValue()); break;
            }
        }
    }

    public void key_down(KeyCode key) {
        int hz = 0;
        prev_key = key;
        hz = switch (key) {
            case Z -> 523;
            case S -> 554;
            case X -> 587;
            case D -> 622;
            case C -> 659;
            case V -> 698;
            case G -> 740;
            case B -> 784;
            case H -> 831;
            case N -> 880;
            case J -> 932;
            case M -> 988;
            case COMMA -> 1047;
            case L -> 1109;
            case PERIOD -> 1175;
            case SEMICOLON -> 1245;
            case SLASH -> 1319;
            case Q -> 1397;
            case DIGIT2 -> 1480;
            case W -> 1568;
            case DIGIT3 -> 1661;
            case E -> 1760;
            case DIGIT4 -> 1865;
            case R -> 1976;
            case T -> 2093;
            case DIGIT6 -> 2217;
            case Y -> 2349;
            case DIGIT7 -> 2489;
            case U -> 2637;
            case I -> 2794;
            case DIGIT9 -> 2960;
            case O -> 3136;
            case DIGIT0 -> 3322;
            case P -> 3520;
            case MINUS -> 3729;
            case OPEN_BRACKET -> 3951;
            case CLOSE_BRACKET -> 4186;
            default -> 0;
        };

        if(hz != 0 && this.socket.isConnected()) {
            try {
                String send_str = "BUZZER_" + hz + "\n";
                byte[] byte_data = send_str.getBytes("UTF-8");
                final var output_stream = this.socket.getOutputStream();
                output_stream.write(byte_data);
                output_stream.flush();
            } catch (UnsupportedEncodingException e) {
                System.out.printf("%s\r\n", e.getMessage());
                System.out.printf("%s\r\n", "인코딩 에러");
            } catch (IOException e) {
                System.out.printf("%s\r\n", e.getMessage());
                System.out.printf("%s\r\n", "OutputStream() 에러");
            }
            if(isRecording) {
                db_insert(hz);
            }
        }
    }

    public void key_up(KeyCode key) {
        if(prev_key.equals(key)) {
            try {
                String send_str = "BUZZER_STOP_\n";
                byte[] byte_data = send_str.getBytes("UTF-8");
                final var output_stream = this.socket.getOutputStream();
                output_stream.write(byte_data);
                output_stream.flush();
            } catch (UnsupportedEncodingException e) {
                System.out.printf("%s\r\n", e.getMessage());
                System.out.printf("%s\r\n", "인코딩 에러");
            } catch (IOException e) {
                System.out.printf("%s\r\n", e.getMessage());
                System.out.printf("%s\r\n", "OutputStream() 에러");
            }
            if(isRecording) {
                db_insert(0);
            }
        }
    }

    public void recording(ActionEvent actionEvent) {
        isRecording ^= true;
        if(isRecording) {
            db_reset();
            System.out.printf("%s\r\n", "Recording Start");
            this.button_record.setText("RECORDING");
            time = System.currentTimeMillis();
            db_conn("INSERT INTO recording VALUES (?, ?, ?, ((SELECT count(id) from recording where id = 1) + 1))");
        } else {
            System.out.printf("%s\t\n", "Recording End");
            this.button_record.setText("RECORD");
        }
    }

    public void playback(ActionEvent actionEvent) {
        if(isRecording) {
            isRecording = false;
        }

        list.clear();

        db_conn("SELECT hz, time from recording where id = 1 ORDER BY turn");
        try {
            ResultSet rs = ps.executeQuery();
            while(rs.next()) {
                list.add(new Buzzer(rs.getInt(1), rs.getInt(2)));
            }
        } catch (SQLException e) {
            System.out.printf("%s\r\n", e.getMessage());
        }

        buzzer_replay();
    }

    public void buzzer_replay() {

        for(Buzzer buzzer: list) {
            try {
                Thread.sleep(buzzer.time);
            } catch (InterruptedException e) {
                System.out.printf("%s\r\n", e.getMessage());
            }
            String send_str;
            if(buzzer.hz == 0) {
                send_str = "BUZZER_STOP_\n";
            } else {
                send_str = "BUZZER_" + buzzer.hz + "\n";
            }

            try {
                byte[] byte_data = send_str.getBytes("UTF-8");
                final var output_stream = this.socket.getOutputStream();
                output_stream.write(byte_data);
                output_stream.flush();
            } catch (UnsupportedEncodingException ex) {
                System.out.printf("%s\r\n", ex.getMessage());
                System.out.printf("%s\r\n", "인코딩 에러");
            } catch (IOException ex) {
                System.out.printf("%s\r\n", ex.getMessage());
                System.out.printf("%s\r\n", "OutputStream() 에러");
            }

        }

    }

    public void db_conn(String sql) {
        try {
            if(ps != null && !ps.isClosed()) {
                ps.close();
            }
            if(conn != null && conn.isClosed()) {
                conn.close();
            }
        } catch (SQLException e) {
            System.out.printf("%s\r\n", e.getMessage());
        }

        final String connectionURL = """
                    jdbc:oracle:thin:@localhost:1521/xe
                    """;
        try {
            Class.forName("oracle.jdbc.OracleDriver");
            conn = DriverManager.getConnection(connectionURL, "WEBMASTER", "webmaster");
            ps = conn.prepareStatement(sql);
        } catch (ClassNotFoundException | SQLException e) {
            System.out.printf("%s\r\n", e.getMessage());
        }
    }

    public void db_insert(int hz) {
        try {
            ps.setInt(1, 1);
            ps.setLong(2, System.currentTimeMillis() - time);
            ps.setInt(3, hz);
            ps.executeUpdate();

            ps.clearParameters();
        } catch (SQLException e) {
            System.out.printf("%s\r\n", e.getMessage());
        }

        time = System.currentTimeMillis();
    }

    public void db_reset() {
        db_conn("DELETE recording where id = ?");
        try {
            ps.setInt(1, 1);
            ps.executeUpdate();

            ps.clearParameters();
        } catch (SQLException e) {
            System.out.printf("%s\r\n", e.getMessage());
        }

    }
}

class Buzzer {
    int hz;
    int time;

    Buzzer(int hz, int time) {
        this.hz = hz;
        this.time = time;
    }
}

//class buzzer_task extends Task<Void> {
//
//    @Override
//    protected Void call() throws Exception {
//        return null;
//    }
//}