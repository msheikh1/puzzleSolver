from flask import Flask, request, jsonify
import torch
import clip
from PIL import Image
import io
import base64

app = Flask(__name__)

device = "cuda" if torch.cuda.is_available() else "cpu"
model, preprocess = clip.load("ViT-B/32", device=device)

@app.route("/classify", methods=["POST"])
def classify_image():
    data = request.get_json()
    if not data or 'image' not in data:
        return jsonify({"error": "No image provided"}), 400

    try:
        # Decode base64 image
        image_data = base64.b64decode(data['image'])
        image = Image.open(io.BytesIO(image_data)).convert("RGB")

        # Preprocess and encode
        image_input = preprocess(image).unsqueeze(0).to(device)

        # Define text labels
        labels = [
            ("sudoku", "A Sudoku puzzle grid with 9x9 boxes"),
            ("nonogram", "A Nonogram puzzle with number clues on top and left sides"),
            ("kakuro", "A Kakuro puzzle with diagonal number clues and empty cells"),
            ("none", "An image that does not contain a puzzle")
        ]

        text_descriptions = [desc for _, desc in labels]
        text_tokens = clip.tokenize(text_descriptions).to(device)

        with torch.no_grad():
            image_features = model.encode_image(image_input)
            text_features = model.encode_text(text_tokens)
            logits_per_image, _ = model(image_input, text_tokens)
            probs = logits_per_image.softmax(dim=-1).cpu().numpy()[0]

        top_index = int(probs.argmax())
        predicted_label, predicted_desc = labels[top_index]
        
        return jsonify({
            "prediction": labels[top_index],
            "confidence": float(probs[top_index])
        })

    except Exception as e:
        return jsonify({"error": str(e)}), 500

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000)
