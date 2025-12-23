import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
  CardDescription,
  Button,
  Badge,
  Textarea,
  useToast, // Using 'useToast' from @mirel/ui as verified in usage patterns
} from '@mirel/ui';
import { ArrowLeft, FileText, Save, Loader2 } from 'lucide-react';
import { apiClient } from '@/lib/api/client';

export default function KnowledgeDocumentViewerPage() {
  const { fileId } = useParams();
  const navigate = useNavigate();
  const { toast } = useToast();
  
  const [loading, setLoading] = useState(true);
  const [content, setContent] = useState('');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (fileId) {
      loadDocument();
    }
  }, [fileId]);

  const loadDocument = async () => {
    setLoading(true);
    try {
        // Fetch content
        const resContent = await apiClient.get(`/api/mira/knowledge/content/${fileId}`);
        setContent(resContent.data);

        // Fetch meta (simulated by list API filter... or just assume from context if passed? 
        // Ideally we need a 'get metadata' API. 
        // For now, we only show content. 
        // We can try to list and filter client side if needed, but for now content is main.)
        // Let's verify scope/owner via list if possible, but LIST endpoint requires scope param.
        // We'll skip deep meta for now and focus on content.
    } catch (error) {
        console.error(error);
        toast({
            title: 'Error',
            description: 'Failed to load document. You might not have permission.',
            variant: 'destructive'
        });
    } finally {
        setLoading(false);
    }
  };

  const handleSave = async () => {
      setSaving(true);
      try {
          await apiClient.put(`/api/mira/knowledge/content/${fileId}`, content, {
             headers: { 'Content-Type': 'text/plain' }
          });
          toast({
              title: 'Saved',
              description: 'Document updated successfully.'
          });
      } catch (error) {
          toast({
              title: 'Error',
              description: 'Failed to update document.',
              variant: 'destructive'
          });
      } finally {
          setSaving(false);
      }
  };

  if (loading) return <div className="flex justify-center p-8"><Loader2 className="w-8 h-8 animate-spin text-muted-foreground" /></div>;

  return (
    <div className="container mx-auto py-6 max-w-4xl space-y-6">
       <div className="flex items-center gap-4">
           <Button variant="ghost" size="sm" onClick={() => navigate(-1)}>
               <ArrowLeft className="w-4 h-4 mr-2" />
               Back
           </Button>
           <h1 className="text-xl font-bold flex items-center gap-2">
               <FileText className="w-5 h-5" />
               Document Viewer
           </h1>
           <Badge variant="outline" className="ml-auto font-mono text-xs text-muted-foreground">
               {fileId}
           </Badge>
       </div>

       <Card>
           <CardHeader>
               <CardTitle>Content Editor</CardTitle>
               <CardDescription>
                   Directly edit the indexed text content. Changes will trigger re-indexing.
               </CardDescription>
           </CardHeader>
           <CardContent className="space-y-4">
               <Textarea 
                  value={content}
                  onChange={(e) => setContent(e.target.value)}
                  className="min-h-[500px] font-mono text-sm"
               />
               
               <div className="flex justify-end gap-2">
                   <Button onClick={handleSave} disabled={saving}>
                       {saving ? <Loader2 className="w-4 h-4 animate-spin mr-2" /> : <Save className="w-4 h-4 mr-2" />}
                       Save Changes
                   </Button>
               </div>
           </CardContent>
       </Card>
    </div>
  );
}
