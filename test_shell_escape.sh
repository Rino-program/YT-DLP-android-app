#!/bin/sh
# シングルクォートエスケープのテスト

# 元のコマンド例
echo "=== テスト1: 通常の引数 ==="
sh -c '/path/to/yt-dlp --no-mtime --newline -o /storage/emulated/0/Download/YtDlp/%(title)s.%(ext)s --no-playlist'

echo ""
echo "=== テスト2: 特殊文字を含むURL ==="
sh -c '/path/to/yt-dlp "https://m.youtube.com/watch?v=VT1on_WaMzw&pp=ygUD5aWP"'

echo ""
echo "=== テスト3: シングルクォートでエスケープ（実装した方法） ==="
sh -c "'/path/to/yt-dlp' '--no-mtime' '--newline' '-o' '/storage/emulated/0/Download/YtDlp/%(title)s.%(ext)s' '--no-playlist' '-f' 'bestvideo[ext=mp4][vcodec^=avc1]+bestaudio[ext=m4a]/best[ext=mp4][vcodec^=avc1]' '--merge-output-format' 'mp4' 'https://m.youtube.com/watch?v=VT1on_WaMzw&pp=ygUD5aWP'"

echo ""
echo "すべての特殊文字 ()[]&^+ がシングルクォート内で保護されます"
