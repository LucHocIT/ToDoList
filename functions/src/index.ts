/**
 * Firebase Functions for ToDoList App
 * Email service for task invitations
 */

import {setGlobalOptions} from "firebase-functions";
import {onRequest} from "firebase-functions/v2/https";
import * as logger from "firebase-functions/logger";
import * as nodemailer from "nodemailer";

// Set global options for cost control
setGlobalOptions({maxInstances: 10});

/**
 * Email configuration interface
 */
interface EmailTemplateParams {
  recipientEmail: string;
  recipientName: string;
  taskTitle: string;
  inviterName: string;
  taskId: string;
  shareId: string;
  inviteUrl: string;
}

/**
 * Create nodemailer transporter
 * @return {nodemailer.Transporter} Email transporter
 */
function createEmailTransporter(): nodemailer.Transporter {
  return nodemailer.createTransport({
    service: "gmail",
    auth: {
      user: process.env.GMAIL_USER || "your-email@gmail.com",
      pass: process.env.GMAIL_APP_PASSWORD || "your-app-password",
    },
  });
}

/**
 * Generate clean unified HTML email template with working button and fallback link
 * @param {EmailTemplateParams} params Email template parameters
 * @return {string} HTML email content
 */
function generateEmailHTML(params: EmailTemplateParams): string {
  // Create web redirect URL that will open the app
  const webRedirectUrl = `https://us-central1-todolist-b2f4a.cloudfunctions.net/redirectToApp?url=${encodeURIComponent(params.inviteUrl)}`;
  
  return `
    <!DOCTYPE html>
    <html>
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Lời mời tham gia Task</title>
        <style>
            body { 
                margin: 0; 
                padding: 20px; 
                font-family: 'Segoe UI', Arial, sans-serif; 
                background-color: #f5f5f5; 
                color: #333; 
            }
            .container { 
                max-width: 600px; 
                margin: 0 auto; 
                background: white; 
                border-radius: 15px; 
                overflow: hidden; 
                box-shadow: 0 5px 25px rgba(0,0,0,0.1); 
            }
            .header { 
                background: linear-gradient(135deg, #4CAF50, #45a049); 
                color: white; 
                padding: 30px 20px; 
                text-align: center; 
            }
            .header h1 { 
                margin: 0; 
                font-size: 24px; 
                font-weight: 600; 
            }
            .content { 
                padding: 40px 30px; 
            }
            .greeting { 
                font-size: 18px; 
                margin-bottom: 20px; 
                color: #333; 
            }
            .inviter { 
                font-weight: 600; 
                color: #4CAF50; 
            }
            .task-box { 
                background: linear-gradient(135deg, #e8f5e8, #f0f8f0); 
                padding: 25px; 
                border-radius: 12px; 
                margin: 25px 0; 
                border-left: 5px solid #4CAF50; 
            }
            .task-title { 
                font-size: 20px; 
                font-weight: 600; 
                color: #2e7d32; 
                margin: 0; 
            }
            .cta-section { 
                text-align: center; 
                margin: 35px 0; 
            }
            .main-button { 
                display: inline-block; 
                padding: 18px 40px; 
                background: linear-gradient(135deg, #4CAF50, #45a049); 
                color: white; 
                text-decoration: none; 
                border-radius: 50px; 
                font-size: 18px; 
                font-weight: 600; 
                box-shadow: 0 4px 15px rgba(76, 175, 80, 0.3); 
                transition: all 0.3s ease; 
            }
            .main-button:hover { 
                transform: translateY(-2px); 
                box-shadow: 0 6px 20px rgba(76, 175, 80, 0.4); 
            }
            .fallback-section { 
                background: #f8f9fa; 
                padding: 20px; 
                border-radius: 10px; 
                margin: 25px 0; 
                text-align: center; 
            }
            .fallback-title { 
                font-size: 14px; 
                color: #666; 
                margin-bottom: 10px; 
            }
            .fallback-link { 
                word-break: break-all; 
                color: #4CAF50; 
                text-decoration: none; 
                font-family: monospace; 
                font-size: 13px; 
                background: white; 
                padding: 10px; 
                border-radius: 5px; 
                border: 1px solid #ddd; 
                display: inline-block; 
                margin: 5px 0; 
            }
            .instructions { 
                background: #fff3cd; 
                border: 1px solid #ffeaa7; 
                border-radius: 8px; 
                padding: 15px; 
                margin: 20px 0; 
                font-size: 14px; 
                color: #856404; 
            }
            .footer { 
                background: #f8f9fa; 
                padding: 25px; 
                text-align: center; 
                color: #666; 
                font-size: 14px; 
                border-top: 1px solid #eee; 
            }
            .divider { 
                height: 2px; 
                background: linear-gradient(to right, transparent, #4CAF50, transparent); 
                margin: 30px 0; 
                border: none; 
            }
        </style>
    </head>
    <body>
        <div class="container">
            <!-- Header -->
            <div class="header">
                <h1>🎯 Lời mời tham gia Task</h1>
            </div>
            
            <!-- Main Content -->
            <div class="content">
                <div class="greeting">
                    Xin chào <strong>${params.recipientName}</strong>! 👋
                </div>
                
                <p>
                    <span class="inviter">${params.inviterName}</span> đã mời bạn tham gia task quan trọng:
                </p>
                
                <!-- Task Info Box -->
                <div class="task-box">
                    <div class="task-title">📝 "${params.taskTitle}"</div>
                </div>
                
                <!-- Call to Action -->
                <div class="cta-section">
                    <p style="margin-bottom: 25px; font-size: 16px;">
                        Nhấn vào nút bên dưới để tham gia ngay lập tức:
                    </p>
                    
                    <a href="${webRedirectUrl}" class="main-button">
                        🚀 Tham gia Task ngay
                    </a>
                </div>
                
                <hr class="divider">
                
                <!-- Instructions -->
                <div class="instructions">
                    <strong>📱 Hướng dẫn:</strong><br>
                    • Nhấn nút "Tham gia Task ngay" để mở ứng dụng tự động<br>
                    • Nếu không có ứng dụng, hãy tải ToDoList từ cửa hàng ứng dụng<br>
                    • Hoặc sử dụng link dự phòng bên dưới
                </div>
                
                <!-- Fallback Link Section -->
                <div class="fallback-section">
                    <div class="fallback-title">
                        🔗 Link dự phòng (copy và dán vào ứng dụng):
                    </div>
                    <a href="${params.inviteUrl}" class="fallback-link">
                        ${params.inviteUrl}
                    </a>
                </div>
            </div>
            
            <!-- Footer -->
            <div class="footer">
                <p>
                    Email này được gửi từ ứng dụng <strong>ToDoList</strong><br>
                    Quản lý công việc thông minh, hiệu quả hơn! ✨
                </p>
                <p style="font-size: 12px; color: #999; margin-top: 15px;">
                    Nếu bạn không mong muốn nhận email này, vui lòng bỏ qua.
                </p>
            </div>
        </div>
    </body>
    </html>
  `;
}

/**
 * Firebase Function to send task invitation email
 * @param {any} request Function request object
 * @return {Promise<any>} Function response
 */
export const sendTaskInvitationEmail = onRequest(async (request, response) => {
  try {
    // Set CORS headers for browser requests
    response.set("Access-Control-Allow-Origin", "*");
    response.set("Access-Control-Allow-Methods", "POST");
    response.set("Access-Control-Allow-Headers", "Content-Type");
    
    if (request.method === "OPTIONS") {
      response.status(204).send("");
      return;
    }
    
    if (request.method !== "POST") {
      response.status(405).send("Method Not Allowed");
      return;
    }
    
    const data = request.body as EmailTemplateParams;

    logger.info("Sending task invitation email", {
      recipient: data.recipientEmail,
      taskTitle: data.taskTitle,
      inviter: data.inviterName,
    });

    // Validate required fields
    const requiredFields = [
      "recipientEmail", "recipientName", "taskTitle", "inviterName",
    ];
    for (const field of requiredFields) {
      if (!data[field as keyof EmailTemplateParams]) {
        throw new Error(`Missing required field: ${field}`);
      }
    }

    // Create transporter
    const transporter = createEmailTransporter();

    // Email options
    const mailOptions = {
      from: `"ToDoList App" <${process.env.GMAIL_USER || 
        "noreply@todolist.com"}>`,
      to: data.recipientEmail,
      subject: `🎯 ${data.inviterName} mời bạn tham gia task: ` +
        `${data.taskTitle}`,
      html: generateEmailHTML(data),
      text: `
Xin chào ${data.recipientName}!

${data.inviterName} đã mời bạn tham gia task: "${data.taskTitle}"

Nhấn vào link này để tham gia: ${data.inviteUrl}

Email này được gửi từ ứng dụng ToDoList.
      `,
    };

    // Send email
    const result = await transporter.sendMail(mailOptions);

    logger.info("Email sent successfully", {
      messageId: result.messageId,
      recipient: data.recipientEmail,
    });

    response.status(200).json({
      success: true,
      message: `Email đã được gửi thành công đến ${data.recipientEmail}`,
      messageId: result.messageId,
    });
  } catch (error) {
    logger.error("Error sending email", error);

    // Return user-friendly error message
    let errorMessage = "Có lỗi xảy ra khi gửi email";

    if (error instanceof Error) {
      if (error.message.includes("Invalid login")) {
        errorMessage = "Lỗi xác thực email. " +
          "Vui lòng kiểm tra cấu hình Gmail.";
      } else if (error.message.includes("Missing required field")) {
        errorMessage = error.message;
      } else {
        errorMessage = `Lỗi gửi email: ${error.message}`;
      }
    }

    response.status(500).json({
      success: false,
      error: errorMessage,
    });
  }
});

/**
 * Redirect function to handle deep links from email
 * @param {any} request Function request object
 * @param {any} response Function response object
 */
export const redirectToApp = onRequest(async (request, response) => {
  try {
    const url = request.query.url as string;
    
    if (!url) {
      response.status(400).send("Missing URL parameter");
      return;
    }

    // Create a simple HTML page that attempts to open the app
    const html = `
      <!DOCTYPE html>
      <html>
      <head>
          <meta charset="UTF-8">
          <title>Mở ứng dụng ToDoList</title>
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <style>
              body {
                  font-family: Arial, sans-serif;
                  max-width: 600px;
                  margin: 50px auto;
                  padding: 20px;
                  text-align: center;
                  background: #f5f5f5;
              }
              .container {
                  background: white;
                  padding: 40px;
                  border-radius: 10px;
                  box-shadow: 0 2px 10px rgba(0,0,0,0.1);
              }
              .button {
                  display: inline-block;
                  padding: 15px 30px;
                  background: #4CAF50;
                  color: white;
                  text-decoration: none;
                  border-radius: 8px;
                  margin: 20px 10px;
                  font-size: 16px;
                  font-weight: bold;
              }
              .button:hover {
                  background: #45a049;
              }
              .fallback {
                  margin-top: 30px;
                  padding: 20px;
                  background: #e8f5e8;
                  border-radius: 5px;
              }
          </style>
      </head>
      <body>
          <div class="container">
              <h1>🎯 ToDoList Task Invitation</h1>
              <p>Nhấn nút bên dưới để mở ứng dụng ToDoList:</p>
              
              <a href="${url}" class="button">📱 Mở ứng dụng</a>
              
              <div class="fallback">
                  <h3>Nếu ứng dụng không mở:</h3>
                  <p>1. Đảm bảo bạn đã cài đặt ứng dụng ToDoList</p>
                  <p>2. Copy link này và mở trong ứng dụng:</p>
                  <p><code>${url}</code></p>
                  <br>
                  <a href="https://play.google.com/store" class="button" style="background: #FF6B6B;">
                      📥 Tải ứng dụng
                  </a>
              </div>
          </div>
          
          <script>
              // Automatically try to open the app
              setTimeout(() => {
                  window.location.href = "${url}";
              }, 1000);
          </script>
      </body>
      </html>
    `;

    response.set("Content-Type", "text/html");
    response.send(html);
  } catch (error) {
    logger.error("Error in redirect function", error);
    response.status(500).send("Internal Server Error");
  }
});