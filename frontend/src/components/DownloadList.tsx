import { DownloadItemDTO } from "../dtos/dtos";

const baseUrl = import.meta.env.VITE_BACKEND_URL;

interface DownloadListProps {
  token: string;
  items: DownloadItemDTO[];
}

const DownloadList = ({ token, items }: DownloadListProps) => {
  return (
    <ul className="flex flex-col gap-4">
      {items.map((item) => (
        <li
          key={item.pieceId}
          className="flex flex-row items-center justify-between gap-8 bg-neutral-900 rounded-lg px-6 py-4"
        >
          <div className="text-left">
            <p className="text-2xl text-white">{item.title}</p>
            <p className="text-lg text-textGray">{item.composer}</p>
          </div>
          <a
            href={`${baseUrl}/download/${token}/${item.pieceId}`}
            className="bg-sky-700 hover:bg-sky-600 text-white text-xl rounded-md px-5 py-2"
            download
          >
            Download PDF
          </a>
        </li>
      ))}
    </ul>
  );
};

export default DownloadList;
