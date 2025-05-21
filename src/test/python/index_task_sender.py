#!/usr/bin/env python3
import pika
import sys
arguments = sys.argv[1:]
if len(arguments) == 2:
    print("Arguments:", arguments)
    id = arguments[0]
    doc_id = arguments[1]
else:
    print("Usage: python3 index_task_sender.py pid docId")
    sys.exit()

connection = pika.BlockingConnection(
    pika.ConnectionParameters(host='localhost'))
channel = connection.channel()

channel.queue_declare(queue='index', durable=True, arguments={'x-max-priority': 10})
channel.queue_bind(exchange='dataone-index',
                   queue='index',
                   routing_key='index')
headers={'index_type': 'create', 'id': id, 'doc_id': doc_id}
properties = pika.BasicProperties(headers=headers)
message = ''
channel.basic_publish(
    exchange='dataone-index',
    routing_key='index',
    body=message,
    properties=properties
    )
print(f" [x] Sent {message}")
connection.close()