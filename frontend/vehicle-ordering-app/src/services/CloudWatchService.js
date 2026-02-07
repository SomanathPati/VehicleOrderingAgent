import { CloudWatchLogsClient, PutLogEventsCommand } from '@aws-sdk/client-cloudwatch-logs';

class CloudWatchServiceClass {
  constructor() {
    this.client = null;
    this.logGroupName = process.env.REACT_APP_CLOUDWATCH_LOG_GROUP || 'vehicle-ordering-frontend';
    this.logStreamName = `frontend-${Date.now()}`;
    this.sequenceToken = null;
  }

  async initialize() {
    if (this.client) return;

    try {
      // AWS configuration - in production, use proper AWS credentials
      this.client = new CloudWatchLogsClient({
        region: process.env.REACT_APP_AWS_REGION || 'us-east-1',
        credentials: {
          accessKeyId: process.env.REACT_APP_AWS_ACCESS_KEY_ID,
          secretAccessKey: process.env.REACT_APP_AWS_SECRET_ACCESS_KEY,
        }
      });

      console.log('CloudWatch client initialized');
    } catch (error) {
      console.error('Failed to initialize CloudWatch client:', error);
      throw error;
    }
  }

  async logEvent(eventType, data) {
    await this.sendLog('INFO', eventType, data);
  }

  async logError(eventType, error) {
    await this.sendLog('ERROR', eventType, error);
  }

  async logWarning(eventType, data) {
    await this.sendLog('WARN', eventType, data);
  }

  async sendLog(level, eventType, data) {
    try {
      if (!this.client) {
        await this.initialize();
      }

      const logEntry = {
        timestamp: Date.now(),
        level,
        eventType,
        data: JSON.stringify(data),
        source: 'frontend-react-app'
      };

      const params = {
        logGroupName: this.logGroupName,
        logStreamName: this.logStreamName,
        logEvents: [{
          message: JSON.stringify(logEntry),
          timestamp: logEntry.timestamp
        }]
      };

      if (this.sequenceToken) {
        params.sequenceToken = this.sequenceToken;
      }

      const command = new PutLogEventsCommand(params);
      const response = await this.client.send(command);

      this.sequenceToken = response.nextSequenceToken;
      console.log(`Log sent to CloudWatch: ${level} - ${eventType}`);

    } catch (error) {
      console.error('Failed to send log to CloudWatch:', error);
      // In production, you might want to fall back to console logging
      // or send to a different logging service
    }
  }

  // Method to create log group and stream (would typically be done via infrastructure)
  async createLogGroupAndStream() {
    // This would typically be handled by CloudFormation/Terraform
    // Implementation would require additional AWS SDK calls
    console.log('Log group and stream creation should be handled by infrastructure');
  }
}

export const CloudWatchService = new CloudWatchServiceClass();
