#include <iostream> // #include <stdio.h> : C
#include <cstring> // #include <string.h> : C 
#include <vector> // vector -> python : LIST
#include <sys/socket.h> // socket(), bind(), listen(), accept()
#include <arpa/inet.h> // 192.168.0.x -> network 값들로 변환해주는 함수가 있음
#include <pthread.h> // thread 를 사용하려는 함수가 있음
#include <array> // C 언어의 배열을 클래스 한것 -> C++ Library 
#include <semaphore.h> // mutex 사용
#include "SerialPort.cpp" // Serial 연결

void* task(void); // main thread
void taskSerial(void); // Serial로 넘어오는 값들을 이 thread 가 처리함
static pthread_mutex_t mutex; // 키가 mutex : binary semaphore
static std::vector<int> client_socket_fds; // 여러개 client 들을 vector 로 처리
static int serial_port = 0; // 시리얼 포트 저장용 변수
static std::array<char, BUFSIZ> tcp_message;
static std::array<char, BUFSIZ> serial_message;

int main(int argc, const char argv[])
{
    int server_sock_fd{0}; // C++ uniform initializer
    int client_sock_fd{0}; // C++ uniform initializer
    struct sockaddr_in server_addr;
    struct sockaddr_in client_addr;
    pthread_t pid {0UL};
    if(argc != 2) {
        std::cout << "./SERVER 9999" << std::endl; // \n
        exit(EXIT_FAILURE); // exit(1)
    }
    serial_port = serialport_init("/dev/ttyACM0", 115200, nullptr); // 시리얼포트
    if(serial_port == -1)
    {
        std::cout << "Serial Port error()" << std::endl;
        exit(EXIT_FAILURE);
    }
    std::cout << "Serial Port is connected ..." << std::endl;
    // mutex 초기화
    pthread_mutex_init(&mutex, nullptr); // mutex

    server_sock_fd = socket(PF_INET /IPv4/, SOCK_STREAM/TCP protocol/, 0);
    if(server_sock_fd == -1) {
        std::cout << "socket() error" << std::endl;
        exit(EXIT_FAILURE);
    }
    memset(&server_addr, 0, sizeof server_addr);
    memset(&client_addr, 0, sizeof client_addr);
    /*서버 주소및 프로토콜 기타등등  값을 대입해준다.*/
  server_addr.sin_family = AF_INET; //IPv4
  server_addr.sin_addr.s_addr = htonl(INADDR_ANY); // 127.0.0.1 or 192.168.0.x
  server_addr.sin_port = htons(atoi(argv[1])); //"9999" -> 9999

    const int bind_state = bind(server_sock_fd, 
            (const struct sockaddr)&server_addr, sizeof server_addr);
    if(bind_state == -1) {
        std::cout << "bind() error" << std::endl;
        exit(EXIT_FAILURE);
    }
    const int listen_state = listen(server_sock_fd, 5);
    if(listen_state == -1)
    {
        std::cout << "listen() error" << std::endl;
        exit(EXIT_FAILURE);
    }
    // 온도, 습도, 값들을 출력하는 thread 생성 (void)&valueOfmainValue
    pthread_create(&pid, nullptr, taskSerial, static_cast<void>(nullptr));
    pthread_detach(pid); // taskSerial join()
    socklen_t client_sock_addr_size {0ul};
    while(true) 
    {
        client_sock_addr_size = sizeof client_addr;
        client_sock_fd = accept(server_sock_fd, (struct sockaddr)&client_addr, 
                (socklen_t)&client_sock_addr_size);
        if(client_sock_fd == -1) {
            std::cout << "accept() error" << std::endl;
            exit(EXIT_FAILURE);
        }
        pthread_mutex_lock(&mutex); // lock
        client_socket_fds.push_back(client_sock_fd); // vector 자료형에 정숫값을 저장
        pthread_mutex_unlock(&mutex); //unlock

        pthread_create(&pid, nullptr, task, static_cast<void>(&client_sock_fd));
        pthread_detach(pid); // join()
        std::cout << "Connected from client IP :  " << inet_ntoa(client_addr.sin_addr)
            << std::endl;

    }
    serialport_close(serial_port);
    close(server_sock_fd);
    pthread_mutex_destroy(&mutex); // destroy mutex 리소스 반납
    return 0;
}
void* taskSerial(void * arg)
{
    while(true)
    {
        const int serial_state {serialport_read_until(serial_port, 
            serial_message.data(), '\n')};
        if(!serial_state)  // 0 성공
        {
            
/*저장되어 있는 모든 클라이언트 들에게 데이터(온도, 습도값들)을  전송*/
          for(auto fd : client_socket_fds) // vector 저장되어 있는 client socket fd{
              pthread_mutex_lock(&mutex); //lock
              write(fd, serial_message.data(), strlen(serial_message.data()));
              pthread_mutex_unlock(&mutex); //unlock}}}
  return nullptr;
}

void* task(void* arg) 
{
    const int clnt_sock_fd = (static_cast<int>(arg)); // (int*)
    int tcp_str_length {0};
    while((tcp_str_length = read(clnt_sock_fd, tcp_message.data(), BUFSIZ))) 
    {
        pthread_mutex_lock(&mutex);
        serialport_write(serial_port, tcp_message.data());
        pthread_mutex_unlock(&mutex);
    } 
/*client 가 종료를 했을 때*/
  pthread_mutex_lock(&mutex);
  for(auto it {client_socket_fds.cbegin()}; it != client_socket_fds.cend(); ++it)
  {
      if(it == clnt_sock_fd) {
          client_socket_fds.erase(it);
          break;}}
  pthread_mutex_unlock(&mutex);std::cout << "A Client has lefted !" << std::endl;std::cout << "client socket has been close !" << std::endl;
  close(clnt_sock_fd);
  return nullptr;
}
