### Overview
AI Tutor is chat bot based on the uploaded document or url. It strictly answer questions about the PDF uploaded and nothing else. 

### Tech Stack
It is created by Integrating Ollama with Spring AI, using pgvector for RAG. 
It uses Vaadin to create a basic UI which allows PDF upload, and provides section for chat bot to initialize any discussion on the document uploaded. 

Model Used:
- chat: gemma3:4b
- embedding: mxbai-embed-large

### How it looks:
<img width="2846" height="1560" alt="image" src="https://github.com/user-attachments/assets/69a090f0-2657-4b9f-8173-e9f0a186b29d" />

### Improvements needed:
- Allow upload of multiple PDFs.
- Make the prompt a bit less strict.
- Add the check if file name already exists in pgvector, do not upload the document to vector db again.
