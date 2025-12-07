import { useState, useEffect } from 'react';

export type OS = 'mac' | 'windows' | 'linux' | 'ios' | 'android' | 'other';

export function useOs() {
  const [os, setOs] = useState<OS>('other');

  useEffect(() => {
    if (typeof window === 'undefined') return;

    const userAgent = window.navigator.userAgent.toLowerCase();
    const platform = window.navigator.platform.toLowerCase();

    if (platform.includes('mac') || userAgent.includes('mac')) {
      setOs('mac');
    } else if (platform.includes('win') || userAgent.includes('win')) {
      setOs('windows');
    } else if (userAgent.includes('android')) {
      setOs('android');
    } else if (userAgent.includes('iphone') || userAgent.includes('ipad') || userAgent.includes('ipod')) {
      setOs('ios');
    } else if (platform.includes('linux') || userAgent.includes('linux')) {
      setOs('linux');
    } else {
      setOs('other');
    }
  }, []);

  const isMac = os === 'mac';
  const metaKey = isMac ? '⌘' : 'Ctrl';
  const metaKeyLabel = isMac ? 'Command' : 'Ctrl';

  return {
    os,
    isMac,
    metaKey,
    metaKeyLabel,
  };
}
