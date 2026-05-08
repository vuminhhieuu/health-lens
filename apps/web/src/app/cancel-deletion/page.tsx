import { Suspense } from 'react';
import CancelDeletionClient from './CancelDeletionClient';

export default function Page() {
  return (
    <Suspense fallback={<div>Loading...</div>}>
      <CancelDeletionClient />
    </Suspense>
  );
}
