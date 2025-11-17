/**
 * コード差分表示コンポーネント
 */
import React, { useMemo } from 'react';
import ReactDiffViewer, { DiffMethod } from 'react-diff-viewer-continued';

interface DiffViewerProps {
  oldValue: string;
  newValue: string;
  oldTitle?: string;
  newTitle?: string;
  splitView?: boolean;
}

export const DiffViewer: React.FC<DiffViewerProps> = ({
  oldValue,
  newValue,
  oldTitle = '変更前',
  newTitle = '変更後',
  splitView = true,
}) => {
  // 差分がない場合の判定
  const hasChanges = useMemo(() => {
    return oldValue !== newValue;
  }, [oldValue, newValue]);

  // 変更行数のカウント
  const changeStats = useMemo(() => {
    const oldLines = oldValue.split('\n');
    const newLines = newValue.split('\n');
    
    let additions = 0;
    let deletions = 0;
    
    // 簡易的な差分カウント（行単位）
    const maxLines = Math.max(oldLines.length, newLines.length);
    for (let i = 0; i < maxLines; i++) {
      if (i >= oldLines.length) {
        additions++;
      } else if (i >= newLines.length) {
        deletions++;
      } else if (oldLines[i] !== newLines[i]) {
        additions++;
        deletions++;
      }
    }
    
    return { additions, deletions };
  }, [oldValue, newValue]);

  if (!hasChanges) {
    return (
      <div className="rounded border border-gray-300 bg-gray-50 p-8 text-center">
        <p className="text-gray-600">変更はありません</p>
      </div>
    );
  }

  return (
    <div className="diff-viewer-container">
      {/* 変更統計 */}
      <div className="mb-2 flex items-center gap-4 text-sm">
        <span className="text-green-600">+{changeStats.additions} 追加</span>
        <span className="text-red-600">-{changeStats.deletions} 削除</span>
      </div>

      {/* 差分表示 */}
      <div className="overflow-auto rounded border border-gray-300">
        <ReactDiffViewer
          oldValue={oldValue}
          newValue={newValue}
          leftTitle={oldTitle}
          rightTitle={newTitle}
          splitView={splitView}
          compareMethod={DiffMethod.WORDS}
          useDarkTheme={false}
          styles={{
            variables: {
              light: {
                diffViewerBackground: '#fff',
                diffViewerColor: '#212529',
                addedBackground: '#e6ffed',
                addedColor: '#24292e',
                removedBackground: '#ffeef0',
                removedColor: '#24292e',
                wordAddedBackground: '#acf2bd',
                wordRemovedBackground: '#fdb8c0',
                addedGutterBackground: '#cdffd8',
                removedGutterBackground: '#ffdce0',
                gutterBackground: '#f6f8fa',
                gutterBackgroundDark: '#f3f4f6',
                highlightBackground: '#fffbdd',
                highlightGutterBackground: '#fff5b1',
              },
            },
            line: {
              padding: '8px 2px',
              fontSize: '13px',
              fontFamily: 'Monaco, Courier, monospace',
            },
          }}
        />
      </div>
    </div>
  );
};
