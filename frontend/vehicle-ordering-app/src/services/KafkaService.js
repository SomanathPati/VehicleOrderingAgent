import { KafkaClient, Producer } from 'kafka-node';

class KafkaServiceClass {
  constructor() {
    this.client = null;
    this.producer = null;
    this.isConnected = false;
  }

  async connect() {
    if (this.isConnected) return;

    return new Promise((resolve, reject) => {
      try {
        // Kafka configuration - in production, these would come from environment variables
        const kafkaHost = process.env.REACT_APP_KAFKA_HOST || 'localhost:9092';
        const topic = process.env.REACT_APP_KAFKA_TOPIC || 'vehicle-orders';

        this.client = new KafkaClient({ kafkaHost });
        this.producer = new Producer(this.client);

        this.producer.on('ready', () => {
          console.log('Kafka producer is ready');
          this.isConnected = true;
          resolve();
        });

        this.producer.on('error', (error) => {
          console.error('Kafka producer error:', error);
          this.isConnected = false;
          reject(error);
        });

      } catch (error) {
        console.error('Failed to connect to Kafka:', error);
        reject(error);
      }
    });
  }

  async sendOrder(orderPayload) {
    if (!this.isConnected) {
      await this.connect();
    }

    return new Promise((resolve, reject) => {
      const topic = process.env.REACT_APP_KAFKA_TOPIC || 'vehicle-orders';

      const payloads = [{
        topic,
        messages: JSON.stringify(orderPayload),
        key: orderPayload.orderId
      }];

      this.producer.send(payloads, (error, data) => {
        if (error) {
          console.error('Error sending message to Kafka:', error);
          reject(error);
        } else {
          console.log('Message sent to Kafka:', data);
          resolve(data);
        }
      });
    });
  }

  disconnect() {
    if (this.client) {
      this.client.close();
      this.isConnected = false;
    }
  }
}

export const KafkaService = new KafkaServiceClass();
