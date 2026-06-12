import { Link, useParams } from "react-router-dom";
import { useEffect, useState } from "react";
import { DownloadManifestDTO } from "../../dtos/dtos";
import DownloadList from "../../components/DownloadList";

const baseUrl = import.meta.env.VITE_BACKEND_URL;

const Downloads = () => {
  const { token } = useParams<{ token: string }>();
  const [manifest, setManifest] = useState<DownloadManifestDTO | null>(null);
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    if (!token) {
      setFailed(true);
      return;
    }

    let isMounted = true;

    async function fetchManifest() {
      try {
        const response = await fetch(`${baseUrl}/download/${token}`);
        if (!response.ok) {
          throw new Error(`Manifest request failed: ${response.status}`);
        }
        const data: DownloadManifestDTO = await response.json();
        if (isMounted) {
          setManifest(data);
        }
      } catch (error) {
        console.error("Error fetching downloads:", error);
        if (isMounted) {
          setFailed(true);
        }
      }
    }

    fetchManifest();

    return () => {
      isMounted = false;
    };
  }, [token]);

  return (
    <div className="flex flex-col">
      <div className="min-h-screen bg-black flex flex-row relative">
        <div className="flex flex-col items-center text-center gap-7 w-full min-h-screen justify-center py-20">
          {failed ? (
            <>
              <h1 className="text-6xl text-white">
                This download link is no longer valid.
              </h1>
              <p className="text-2xl text-white m-3">
                Links expire after a while for security. Email us at
                SebastianHavner@gmail.com and we will send you a fresh one.
              </p>
            </>
          ) : manifest ? (
            <>
              <h1 className="text-6xl text-white">Your purchased pieces</h1>
              <DownloadList token={token!} items={manifest.items} />
              <p className="text-xl text-textGray m-3">
                This link is valid until{" "}
                {new Date(manifest.expiresAt).toLocaleDateString()}.
              </p>
            </>
          ) : (
            <p className="text-3xl text-white m-3">Loading your downloads...</p>
          )}
          <span className="text-textGray text-2xl">
            <p>
              Feel free to continue browsing our{" "}
              <Link to={"/"} className="text-sky-700 underline">
                website.
              </Link>{" "}
            </p>
          </span>
        </div>
      </div>
    </div>
  );
};

export default Downloads;
